
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

```ref-database.storeMutableRefs```
:   Specifies which projects should have mutable refs stored. An asterisk can be
    used to match all projects. Excludes draft comments, immutable non-meta refs,
    and cache-automerge refs. An asterisk can be used to match all projects.

<<<<<<< PATCH SET (75fc1d3a5b191a7e5a66fa8b15c836f1b52e4206 Prioritize per-project over global ref storage settings)
    Excludes draft comments, immutable non-meta refs, and cache-automerge refs.

    Details: An asterisk can be used to match all projects. Storage rules are
    evaluated in the following order: project-specific settings (storeNoRefs, then
    storeMutableRefs, then storeAllRefs), followed by global settings (using * as
    a wildcard) in the same order.
||||||| BASE
    Excludes draft comments, immutable non-meta refs, and cache-automerge refs.

    Details: An asterisk can be used to match all projects.
=======
    Defaults: No rules = All projects store mutable refs.
>>>>>>> BASE      (4b407c068ee02dc15b98c30bb5576b5bec59865b Add StoreMutableRefs as label for default behavior)

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