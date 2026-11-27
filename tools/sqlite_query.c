#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>

typedef struct sqlite3 sqlite3;
typedef int (*fn_open)(const char*, sqlite3**);
typedef int (*fn_close)(sqlite3*);
typedef int (*fn_exec)(sqlite3*, const char*, int (*)(void*,int,char**,char**), void*, char**);

static int print_cb(void* data, int argc, char** argv, char** colNames) {
    for (int i = 0; i < argc; i++) {
        if (i > 0) printf("|");
        printf("%s", argv[i] ? argv[i] : "NULL");
    }
    printf("\n");
    return 0;
}

int main(int argc, char** argv) {
    if (argc < 3) {
        fprintf(stderr, "Usage: %s <db_path> <query>\n", argv[0]);
        return 1;
    }

    void* handle = dlopen("/system/lib64/libsqlite.so", RTLD_NOW);
    if (!handle) handle = dlopen("libsqlite.so", RTLD_NOW);
    if (!handle) return 1;

    fn_open s_open = (fn_open)dlsym(handle, "sqlite3_open");
    fn_close s_close = (fn_close)dlsym(handle, "sqlite3_close");
    fn_exec s_exec = (fn_exec)dlsym(handle, "sqlite3_exec");
    if (!s_open || !s_close || !s_exec) {
        dlclose(handle);
        return 2;
    }

    sqlite3* db = NULL;
    if (s_open(argv[1], &db) != 0 || !db) {
        dlclose(handle);
        return 3;
    }

    char* err = NULL;
    s_exec(db, argv[2], print_cb, NULL, &err);

    s_close(db);
    dlclose(handle);
    return 0;
}
