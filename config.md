
global-refdb Configuration
=========================

Configuration should be specified in the `$site_path/etc/<libModule>.config` file of
the libModule consuming this library.

## Configuration parameters

```ref-database.enabled```
:   Enable the use of a global refdb
    Defaults: true

```ref-database.enforcementRules```
:   Specifies which refs will be included in the global-refdb.

    Rules apply in order top-down, such that the first matched rule is the one
    to take effect. Default rules apply after custom rules. Rules support
    project scopes and wildcard matching.

    The default behavior is to exclude draft comments, immutable non-meta refs,
    and cache-automerge refs:

    `refs/draft-comments/*`
    `refs/changes/*` except `refs/changes/.../meta`
    `refs/cache-automerge/*`

    To store all refs for all projects in the global-refdb:
    ```
    [ref-database "enforcementRules"]
      rule = INCLUDE:*:*
    ```
    To store all draft-comments except those of a specific project:
    ```
    [ref-database "enforcementRules"]
      rule = EXCLUDE:my-repo:refs/draft-comments/*
      rule = INCLUDE:*:refs/draft-comments/*
    ```
    Note the nuance: The uppermost matching rule is evaluated first, so despite
    both rules applying to a draft comment in my-repo, it is excluded due to
    matching the first rule.

    If the rules were the other way around:
    ```
    [ref-database "enforcementRules"]
      rule = INCLUDE:*:refs/draft-comments/*
      rule = EXCLUDE:my-repo:refs/draft-comments/*
    ```
    This configuration would include all draft comments and the second rule
    would not be effective.

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