
global-refdb Configuration
=========================

Configuration should be specified in the `$site_path/etc/<libModule>.config` file of
the libModule consuming this library.

## Configuration parameters

```ref-database.enabled```
:   Enable the use of a global refdb
    Defaults: true

```ref-database.enforcementRules.<policy>```
:   Level of consistency enforcement across sites on a project:refs basis.
    Supports two values for enforcing the policy on multiple projects or refs.
    If the project or ref is omitted, apply the policy to all projects or all refs.

    The <policy> can have one of the following values:

    1. REQUIRED - Throw an exception if a git ref-update is processed against
    a local ref not yet in sync with the global refdb.
    The user transaction is cancelled.

    2. IGNORED - Ignore any validation against the global refdb.

    *Example:*
    ```
    [ref-database "enforcementRules"]
       IGNORED = AProject:/refs/heads/feature
    ```

    Ignore the alignment with the global refdb for AProject on refs/heads/feature.

    Defaults: No rules = All projects are REQUIRED to be consistent on all refs.

    Warning: This configuration is deprecated in favor of the storeAllRefs/
    storeNoRefs described below. Existing configured enforcementRules will work,
    but not in tandem with the storeAllRefs/storeNoRefs paradigm.

```ref-database.storeAllRefs```
:   Specifies which projects should have all refs stored, including refs which
    are excluded by default. An asterisk can be used to match all projects.

    The default behavior for non-specified projects is to exclude draft comments,
    immutable non-meta refs, and cache-automerge refs. When listed under
    `storeAllRefs` those refs will be included.

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