#include <dirent.h>
#include <fcntl.h>
#include <linux/hidraw.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <android/log.h>

#include <cerrno>
#include <chrono>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <string>
#include <vector>

#define TAG "xiaomi-pen-pressure"
#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__); } while (0)
#define LOGW(...) do { __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__); } while (0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__); } while (0)
#define PLOGE(fmt, ...) do { __android_log_print(ANDROID_LOG_ERROR, TAG, fmt ": %s", ##__VA_ARGS__, strerror(errno)); } while (0)

namespace {

struct unique_fd {
    int fd = -1;
    unique_fd() : fd(-1) {}
    explicit unique_fd(int f) : fd(f) {}
    ~unique_fd() { reset(); }
    void reset(int f = -1) {
        if (fd >= 0) close(fd);
        fd = f;
    }
    int get() const { return fd; }
    operator int() const { return fd; }
    unique_fd(const unique_fd&) = delete;
    unique_fd& operator=(const unique_fd&) = delete;
    unique_fd(unique_fd&& o) noexcept : fd(o.fd) { o.fd = -1; }
    unique_fd& operator=(unique_fd&& o) noexcept {
        reset(o.fd);
        o.fd = -1;
        return *this;
    }
};

constexpr const char* kMippDev = "/dev/mipp_pen0";
constexpr const char* kUinputDev = "/dev/uinput";
constexpr const char* kIcPenName = "NVTCapacitivePen";
constexpr const char* kVirtualPenName = "NVTCapacitivePen (ruan)";
constexpr uint8_t kPressureReportId = 0x05;
constexpr size_t kMippDataLength = 15;

constexpr uint16_t kPenVendorId = 0x0022;
constexpr uint16_t kPenProductIds[] = {0x4e83, 0x3283};

constexpr int kRescanMs = 1000;
constexpr const char* kActiveProp = "vendor.pen.active";
constexpr std::chrono::milliseconds kInactiveAfter{3000};

using Clock = std::chrono::steady_clock;

class Activity {
  public:
    void reportSeen() {
        last_ = Clock::now();
        set(true);
    }

    int timeoutMs() const {
        if (!active_) return -1;
        auto left = std::chrono::duration_cast<std::chrono::milliseconds>(
                last_ + kInactiveAfter - Clock::now());
        return left.count() > 0 ? static_cast<int>(left.count()) : 0;
    }

    void update() {
        if (active_ && Clock::now() - last_ >= kInactiveAfter) set(false);
    }

    void set(bool active) {
        if (active == active_) return;
        active_ = active;
        __system_property_set(kActiveProp, active ? "1" : "0");
    }

  private:
    bool active_ = false;
    Clock::time_point last_;
};

constexpr unsigned kDefaultDeadZone = 280;
constexpr int kAbsAxes[] = {ABS_X, ABS_Y, ABS_PRESSURE, ABS_DISTANCE, ABS_TILT_X, ABS_TILT_Y};

bool isPen(const hidraw_devinfo& info) {
    if (info.bustype != BUS_BLUETOOTH || static_cast<uint16_t>(info.vendor) != kPenVendorId) {
        return false;
    }
    for (uint16_t product : kPenProductIds) {
        if (static_cast<uint16_t>(info.product) == product) {
            return true;
        }
    }
    return false;
}

unique_fd openPenHidraw() {
    std::unique_ptr<DIR, decltype(&closedir)> dir(opendir("/dev"), closedir);
    if (!dir) {
        PLOGE("opendir /dev");
        return {};
    }
    while (dirent* entry = readdir(dir.get())) {
        if (strncmp(entry->d_name, "hidraw", 6) != 0) {
            continue;
        }
        std::string path = std::string("/dev/") + entry->d_name;
        unique_fd fd(open(path.c_str(), O_RDONLY | O_CLOEXEC));
        if (fd < 0) {
            continue;
        }
        hidraw_devinfo info = {};
        if (ioctl(fd, HIDIOCGRAWINFO, &info) == 0 && isPen(info)) {
            LOGI("pen pressure: reading %s", path.c_str());
            return fd;
        }
    }
    return {};
}

unique_fd openIcPen() {
    std::unique_ptr<DIR, decltype(&closedir)> dir(opendir("/dev/input"), closedir);
    if (!dir) {
        PLOGE("opendir /dev/input");
        return {};
    }
    while (dirent* entry = readdir(dir.get())) {
        if (strncmp(entry->d_name, "event", 5) != 0) {
            continue;
        }
        std::string path = std::string("/dev/input/") + entry->d_name;
        unique_fd fd(open(path.c_str(), O_RDONLY | O_CLOEXEC));
        if (fd < 0) {
            continue;
        }
        char name[64] = {};
        if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), name) >= 0 && !strcmp(name, kIcPenName)) {
            LOGI("pen contact: using %s", path.c_str());
            return fd;
        }
    }
    return {};
}

struct PenState {
    int x = 0, y = 0, tiltX = 0, tiltY = 0, distance = 0;
    bool inRange = false, button1 = false, button2 = false;
    unsigned pressure = 0;
};

class VirtualPen {
  public:
    bool create(int icFd) {
        fd_.reset(open(kUinputDev, O_WRONLY | O_CLOEXEC));
        if (fd_ < 0) {
            PLOGE("open %s", kUinputDev);
            return false;
        }
        input_id id = {};
        ioctl(icFd, EVIOCGID, &id);
        bool ok = ioctl(fd_, UI_SET_EVBIT, EV_KEY) == 0 && ioctl(fd_, UI_SET_EVBIT, EV_ABS) == 0 &&
                  ioctl(fd_, UI_SET_PROPBIT, INPUT_PROP_DIRECT) == 0;
        for (int key : {BTN_TOOL_PEN, BTN_TOUCH, BTN_STYLUS, BTN_STYLUS2}) {
            ok = ok && ioctl(fd_, UI_SET_KEYBIT, key) == 0;
        }
        for (int axis : kAbsAxes) {
            uinput_abs_setup abs = {};
            abs.code = axis;
            if (ioctl(icFd, EVIOCGABS(axis), &abs.absinfo) < 0) {
                PLOGE("EVIOCGABS %d", axis);
                return false;
            }
            abs.absinfo.value = 0;
            ok = ok && ioctl(fd_, UI_SET_ABSBIT, axis) == 0 && ioctl(fd_, UI_ABS_SETUP, &abs) == 0;
        }
        uinput_setup setup = {};
        setup.id = id;
        setup.id.bustype = BUS_SPI;
        strncpy(setup.name, kVirtualPenName, UINPUT_MAX_NAME_SIZE - 1);
        ok = ok && ioctl(fd_, UI_DEV_SETUP, &setup) == 0 && ioctl(fd_, UI_DEV_CREATE) == 0;
        if (!ok) {
            PLOGE("uinput setup");
            fd_.reset();
        }
        return ok;
    }

    void report(const PenState& s) {
        bool ink = s.inRange && s.distance == 0;
        unsigned pressure = ink ? s.pressure : 0;
        bool touch = pressure > 0;
        events_.clear();
        add(EV_ABS, ABS_X, s.x);
        add(EV_ABS, ABS_Y, s.y);
        add(EV_ABS, ABS_TILT_X, s.tiltX);
        add(EV_ABS, ABS_TILT_Y, s.tiltY);
        add(EV_ABS, ABS_PRESSURE, static_cast<int>(pressure));
        add(EV_ABS, ABS_DISTANCE, touch ? 0 : 1);
        add(EV_KEY, BTN_TOOL_PEN, s.inRange);
        add(EV_KEY, BTN_TOUCH, touch);
        add(EV_KEY, BTN_STYLUS, s.button1);
        add(EV_KEY, BTN_STYLUS2, s.button2);
        add(EV_SYN, SYN_REPORT, 0);
        if (write(fd_, events_.data(), events_.size() * sizeof(input_event)) < 0) {
            PLOGE("write %s", kUinputDev);
        }
    }

    bool valid() const { return fd_ >= 0; }

  private:
    void add(uint16_t type, uint16_t code, int value) {
        input_event ev = {};
        ev.type = type;
        ev.code = code;
        ev.value = value;
        events_.push_back(ev);
    }

    unique_fd fd_;
    std::vector<input_event> events_;
};

bool applyIcEvent(const input_event& ev, PenState& s) {
    if (ev.type == EV_ABS) {
        switch (ev.code) {
            case ABS_X: s.x = ev.value; break;
            case ABS_Y: s.y = ev.value; break;
            case ABS_TILT_X: s.tiltX = ev.value; break;
            case ABS_TILT_Y: s.tiltY = ev.value; break;
            case ABS_DISTANCE: s.distance = ev.value; break;
            default: break;
        }
    } else if (ev.type == EV_KEY) {
        switch (ev.code) {
            case BTN_TOOL_PEN: s.inRange = ev.value; break;
            case BTN_STYLUS: s.button1 = ev.value; break;
            case BTN_STYLUS2: s.button2 = ev.value; break;
            default: break;
        }
    } else if (ev.type == EV_SYN && ev.code == SYN_REPORT) {
        return true;
    }
    return false;
}

void resync(int icFd, PenState& s) {
    input_absinfo abs = {};
    if (ioctl(icFd, EVIOCGABS(ABS_X), &abs) == 0) s.x = abs.value;
    if (ioctl(icFd, EVIOCGABS(ABS_Y), &abs) == 0) s.y = abs.value;
    if (ioctl(icFd, EVIOCGABS(ABS_TILT_X), &abs) == 0) s.tiltX = abs.value;
    if (ioctl(icFd, EVIOCGABS(ABS_TILT_Y), &abs) == 0) s.tiltY = abs.value;
    if (ioctl(icFd, EVIOCGABS(ABS_DISTANCE), &abs) == 0) s.distance = abs.value;
    uint8_t keys[KEY_MAX / 8 + 1] = {};
    if (ioctl(icFd, EVIOCGKEY(sizeof(keys)), keys) >= 0) {
        auto pressed = [&](int code) { return (keys[code / 8] >> (code % 8)) & 1; };
        s.inRange = pressed(BTN_TOOL_PEN);
        s.button1 = pressed(BTN_STYLUS);
        s.button2 = pressed(BTN_STYLUS2);
    }
}

}  // namespace

int main(int argc, char** argv) {
    unsigned deadZone = argc > 1 ? static_cast<unsigned>(atoi(argv[1])) : kDefaultDeadZone;
    LOGI("pen pressure: dead zone %u", deadZone);

    unique_fd mipp(open(kMippDev, O_WRONLY | O_CLOEXEC));
    if (mipp < 0) {
        PLOGE("open %s", kMippDev);
        return 1;
    }

    VirtualPen virtualPen;
    unique_fd ic = openIcPen();
    if (ic >= 0 && virtualPen.create(ic)) {
        if (ioctl(ic, EVIOCGRAB, 1) < 0) {
            PLOGE("grab %s", kIcPenName);
            ic.reset();
        }
    } else {
        ic.reset();
    }
    if (ic < 0) {
        LOGW("pen contact: left to the touch driver");
    }

    PenState state;
    if (ic >= 0) {
        resync(ic, state);
    }

    unique_fd pen;
    bool logged = false;
    Activity activity;
    __system_property_set(kActiveProp, "0");

    for (;;) {
        if (pen < 0) {
            pen = openPenHidraw();
            logged = false;
        }
        pollfd fds[2] = {{ic.get(), POLLIN, 0}, {pen.get(), POLLIN, 0}};
        int timeout = activity.timeoutMs();
        if (pen < 0 && (timeout < 0 || timeout > kRescanMs)) {
            timeout = kRescanMs;
        }
        int ret = poll(fds, 2, timeout);
        activity.update();
        if (ret < 0) {
            if (errno == EINTR) {
                continue;
            }
            PLOGE("poll");
            return 1;
        }

        if (fds[0].revents & POLLIN) {
            input_event events[64];
            ssize_t len = read(ic, events, sizeof(events));
            for (ssize_t i = 0; i < len / static_cast<ssize_t>(sizeof(input_event)); i++) {
                if (events[i].type == EV_SYN && events[i].code == SYN_DROPPED) {
                    resync(ic, state);
                } else if (applyIcEvent(events[i], state)) {
                    virtualPen.report(state);
                }
            }
        }

        if (fds[1].revents & (POLLIN | POLLERR | POLLHUP)) {
            uint8_t report[64];
            ssize_t len = read(pen, report, sizeof(report));
            if (len < 0 && errno == EINTR) {
                continue;
            }
            if (len <= 0) {
                LOGI("pen pressure: hidraw closed");
                pen.reset();
                activity.set(false);
                state.pressure = 0;
                if (virtualPen.valid() && ic >= 0) {
                    virtualPen.report(state);
                }
                continue;
            }
            if (report[0] != kPressureReportId || static_cast<size_t>(len) < 3) {
                continue;
            }
            activity.reportSeen();
            if (!logged) {
                LOGI("pen pressure: first report, %zd bytes", len);
                logged = true;
            }
            unsigned pressure = report[1] | (report[2] << 8);
            pressure = pressure > deadZone ? pressure - deadZone : 0;
            report[1] = pressure & 0xff;
            report[2] = pressure >> 8;
            size_t out = static_cast<size_t>(len) < kMippDataLength ? len : kMippDataLength;
            if (write(mipp, report, out) < 0) {
                PLOGE("write %s", kMippDev);
            }
            if (state.pressure != pressure) {
                state.pressure = pressure;
                if (ic >= 0 && state.inRange) {
                    virtualPen.report(state);
                }
            }
        }
    }
}
