#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/ioctl.h>
#include <linux/input.h>

static int find_touch_fd(void) {
    DIR *dir = opendir("/sys/class/input");
    if (!dir) return -1;
    struct dirent *ent;
    char path[256], buf[128];
    int touch_fd = -1;
    while ((ent = readdir(dir)) != NULL) {
        if (strncmp(ent->d_name, "event", 5) != 0) continue;
        snprintf(path, sizeof(path), "/sys/class/input/%s/device/name", ent->d_name);
        int fd = open(path, O_RDONLY);
        if (fd < 0) continue;
        int n = read(fd, buf, sizeof(buf) - 1);
        close(fd);
        if (n > 0) {
            buf[n] = '\0';
            if (strstr(buf, "NVTCapacitiveTouchScreen")) {
                snprintf(path, sizeof(path), "/dev/input/%s", ent->d_name);
                touch_fd = open(path, O_RDWR);
                break;
            }
        }
    }
    closedir(dir);
    return touch_fd;
}

int main(void) {
    // 1. Touch device ioctl fallback
    int dev_fd = open("/dev/xiaomi-touch", O_RDWR);
    if (dev_fd >= 0) {
        int arg[3] = {0, 14, 1};
        ioctl(dev_fd, 0x5400, arg);
        close(dev_fd);
    }

    // 2. Open input event node and inject EV_SYN(0)/SYN_CONFIG(1)/WAKEUP_ON(5)
    int fd = -1;
    for (int retry = 0; retry < 60; retry++) {
        fd = find_touch_fd();
        if (fd >= 0) break;
        sleep(1);
    }
    if (fd < 0) return 1;

    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = EV_SYN;
    ev.code = 1; // SYN_CONFIG
    ev.value = 5; // WAKEUP_ON
    write(fd, &ev, sizeof(ev));

    // Periodic re-arming loop
    while (1) {
        sleep(10);
        write(fd, &ev, sizeof(ev));
    }
    return 0;
}
