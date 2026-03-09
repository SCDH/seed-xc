# Contributing

## Code formatting

This project uses
[`clang-format`](https://clang.llvm.org/docs/ClangFormat.html) for
formatting Java code. `clang-format` is part of the [LLVM compiler
infrastructure](https://llvm.org/) which is available for Linux as
well as for Windows and Mac OS. `clang-format` has good support for
formatting Java and integrates with git and/or editors like Vim,
Emacs, or VSCode.

Formatting from the command line (all files):

```shell
find . -type f -name '*.java' -exec clang-format -i {} +
```

Formatting staged changes:

```shell
git clang-format
```

Editor integration:
https://clang.llvm.org/docs/ClangFormat.html#vim-integration


The formatting configuration is in `.clang-format`,
