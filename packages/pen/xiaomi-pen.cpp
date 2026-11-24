#include <cstring>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <cstdio>
#include <cerrno>
#include <android/log.h>

#define TAG "xiaomi-pen"
#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__); printf(__VA_ARGS__); printf("\n"); } while (0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__); fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); } while (0)
#define PLOGE(fmt, ...) do { __android_log_print(ANDROID_LOG_ERROR, TAG, fmt ": %s", ##__VA_ARGS__, strerror(errno)); fprintf(stderr, fmt ": %s\n", ##__VA_ARGS__, strerror(errno)); } while (0)

namespace {

constexpr const char* kTouchDev = "/dev/xiaomi-touch";
constexpr int kSetCurValue = _IO('t', 0);
constexpr int kTouchPenEnable = 20;
constexpr size_t kMaxBufSize = 256;

constexpr int kPenGeneration = 4;
constexpr int kConnected = 0x10 | kPenGeneration;
constexpr int kDisconnected = kPenGeneration;
constexpr int kReset = -1;

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

}  // namespace

int main(int argc, char** argv) {
    if (argc != 2) {
        LOGE("usage: xiaomi-pen connected|disconnected|reset");
        return 1;
    }

    int value;
    if (!strcmp(argv[1], "connected")) {
        value = kConnected;
    } else if (!strcmp(argv[1], "disconnected")) {
        value = kDisconnected;
    } else if (!strcmp(argv[1], "reset")) {
        value = kReset;
    } else {
        LOGE("unknown pen state %s", argv[1]);
        return 1;
    }

    unique_fd fd(open(kTouchDev, O_RDWR | O_CLOEXEC));
    if (fd < 0) {
        PLOGE("open %s", kTouchDev);
        return 1;
    }

    int buf[kMaxBufSize] = {0, kTouchPenEnable, value};
    if (ioctl(fd, kSetCurValue, buf) < 0) {
        PLOGE("set pen mode %d", value);
        return 1;
    }

    LOGI("pen state %s (mode %d = %d)", argv[1], kTouchPenEnable, value);
    return 0;
}
