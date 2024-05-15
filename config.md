
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

```ref-database.storageRules```
:   Specifies which refs will be included in the global-refdb based on regular
    expressions. This is evaluated on a top-down basis, where the first rule with
    matching pattern applies to a given ref. The last rules to be checked are the
    default exclusions:

    1. `refs/draft-comments/*`
    2. `refs/changes/*` except `refs/changes/.../meta`: immutable refs
    3. `refs/cache-automerge/*`

    User-provided rules can override the default behavior.

    To store all refs in the global-refdb:
    ```
    [ref-database "storageRules"]
      rule = INCLUDE:^.*
    ```
    To store all draft-comments, then exclude any remaining refs containing the
    string 'test':
    ```
    [ref-database "storageRules"]
      rule = INCLUDE:^refs/draft-comments/.*
      rule = EXCLUDE:^.*test.*
    ```
    Note the nuance: The uppermost matching rule is evaluated first, so a draft
    comment that happened to contain the term 'test' in its ref would be included.
    If the desired behavior is the other way around, the rule order should be:
    ```
    [ref-database "storageRules"]
      rule = EXCLUDE:^.*test.*
      rule = INCLUDE:^refs/draft-comments/.*
    ```
    This configuration would exclude all refs containing 'test', then include
    all remaining draft-comments.

    Any refs that do not match either custom or default rules are stored.

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