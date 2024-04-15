# Gerrit interface to a global-refdb

Global ref-database interface for use with Gerrit Code Review.

Enables the de-coupling between Gerrit, its libModules and the different
implementations of a globally shared ref-database.

## Design

[The design for a global ref-db interface](https://gerrit.googlesource.com/plugins/multi-site/+/refs/heads/master/DESIGN.md#global-ref_db-plugin)
can be found as part of the multi-site design documentation, where it first
originated and was approved by the community.

## Bindings

In order to consume this library, some Guice bindings need to be registered
appropriately. More information in the relevant [documentation](./bindings.md).

## Metrics

Global ref-database expose metrics to measure the global ref-database operation latency.
List of the available metrics can be found [here](./metrics.md).

## How to build

This libModule is built like a Gerrit in-tree plugin, using Bazelisk.

### Build in Gerrit tree

Create a symbolic link of the repository source to the Gerrit source tree /plugins/global-refdb directory.

Example:

```
git clone https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/modules/global-refdb
cd gerrit/plugins
ln -s ../../global-refdb .
```

From the Gerrit source tree issue the command bazelisk build plugins/global-refdb

Example:

```
bazelisk build plugins/global-refdb
```

The libModule jar file is created under bazel-bin/plugins/global-refdb/global-refdb.jar

To execute the tests run bazelisk test plugins/global-refdb/... from the Gerrit source tree.

Example:

```
bazelisk test plugins/global-refdb/...
```

## How to import into Eclipse as a project

Add `global-refdb` in the `CUSTOM_PLUGINS` section of the `tools/bzl/plugins.bzl`.

Example:

```
CUSTOM_PLUGINS = [
    "global-refdb",
]
```

Run `tools/eclipse/project.py` for generating or updating the Eclipse project.