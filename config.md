
global-refdb Configuration
=========================

Configuration should be specified in the `$site_path/etc/<libModule>.config` file of
the libModule consuming this library.

## Configuration parameters

```ref-database.enabled```
:   Enable the use of a global refdb
    Defaults: true

```ref-database.storeMutableRefs```
:   Specifies which projects should have mutable refs stored. An asterisk can be
    used to match all projects. This is the default behavior for all projects not
    specified under storeAllRefs or storeNoRefs. 

    Excludes draft comments, immutable non-meta refs, and cache-automerge refs.

    Details: An asterisk can be used to match all projects. A project can only be
    in one ref storage category (storeMutableRefs, storeAllRefs, or storeNoRefs).

```ref-database.storeAllRefs```
:   Specifies which projects should have all refs stored, including refs which
    are excluded by default (draft comments, immutable non-meta refs, and cache-
    automerge refs). See ```ref-database.storeMutableRefs``` for more details.

```ref-database.storeNoRefs```
:   Specifies which projects should not be stored in the global-refdb. No refs
    from these projects will be stored. An asterisk can be used to match all
    projects. If a project is in both storeNoRefs and storeAllRefs, it will not
    be stored; the order of processing is storeNoRefs then storeAllRefs.

```projects.pattern```
:   Specifies which projects should be validated against the global refdb.
    It can be provided more than once, and supports three formats: regular
    expressions, wildcard matching, and single project matching. All three
    formats match case-sensitive.

    Values starting with a caret `^` are treated as regular
    expressions. For the regular expressions details please follow
    official [java documentation](https://docs.oracle.com/javase/tutorial/essential/regex/).

    Please note that regular expressions could also be used
    with inverse match.

    Values that are not regular expressions and end in `*` are
    treated as wildcard matches. Wildcards match projects whose
    name agrees from the beginning until the trailing `*`. So
    `foo/b*` would match the projects `foo/b`, `foo/bar`, and
    `foo/baz`, but neither `foobar`, nor `bar/foo/baz`.

    Values that are neither regular expressions nor wildcards are
    treated as single project matches. So `foo/bar` matches only
    the project `foo/bar`, but no other project.

    By default, all projects are matched.