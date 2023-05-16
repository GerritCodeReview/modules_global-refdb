package(default_visibility = ["//visibility:public"])

load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_DEPS_NEVERLINK",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "global-refdb",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = [],
)

junit_tests(
    name = "global-refdb_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["global-refdb"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":global-refdb",
    ],
)
