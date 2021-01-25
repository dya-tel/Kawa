# Kawa
Kawa is a simple DataFlow analyzer for Java code built upon [Spoon](https://github.com/INRIA/spoon) metamodel. It only supports value tracking, object references and simple boolean calculations for now. Some code elements (string concatenation, for example) are even skipped completely and it sometimes impacts the results. Any unsupported element is dumped to the logger when it is encountered for the first time.

In theory, Kawa can be adapted for any AST, but no effort to support this use case out-of-box was (or will be) made. If you for some reason really want to use Kawa this way, you should convert your AST to the Spoon model and feed it to Kawa as usual.

Kawa is built with modularity, readability and testability in mind. It makes heavy use of composition and not inheritance which helps a lot. There is no shared state so it is perfectly safe in multithreaded environments unless Spoon decides to explode.

## Constraint inferring
Kawa is capable of inferring constraints from conditions:
```java
if (x != null && y != null) {
    // x is not null
    // y is not null
} else {
    // x is possible null
    // y is possible null
}

if (x == null) {
    return;
}
// x is not null
```

Currently, inferred constraints are dirtying the following flow:
```java
public void doSomething(String x) {
    // x nullability is unknown
    if (x == null) {
        ...
    }
    // x is a possible null
}
```

While this might seem as a valid assumption in theory, in reality it's often not and it leads to a lot of strange and sometimes even invalid results. The nullability status should still be "unknown". Any suggestions for fixing this behavior are welcome!

## Interprocedural analysis
Interprocedural analysis support is still pretty raw, but in some cases it can provide a lot of useful information.

Kawa's interprocedural analysis has two operation modes:
1) Annotation-based (`BasicMethodEmulator` class). It's fast, it's simple, it's stupid. It makes a lot of wrong assumptions because the auto-generated annotations are *very* simple.
2) Inlining-based (`InliningMethodEmulator` class). It's slower, but far smarter. It basically emulates the called code every time so no information is lost.

Current operation mode can be selected in `MethodFlowAnalyzer` by changing the `interproceduralEmulator` property. Annotation-based mode is the default one.

Interprocedural analysis is only used when the exact called method is 100% known, implemented and can't be overridden. Those conditions are satisfied mostly by static, final and private methods.

Manual method annotations are not supported yet.

# Bundled static analyzer
Kawa also includes an ultra-simple static analyzer in the "Launcher" module for showcasing current capabilities and ensuring that it works at all.

`FlowLauncher` is the main class. To run the analyzer, you must supply at least two arguments: project path and one (or more) source paths. Assuming you are trying to analyze a project in directory "test-project": `test-project test-project/module1 test-project/module2`. Source paths **must** use Maven layout (sources are located in `test-project/module1/src/main/java` and `test-project/module2/src/main/java` in this example).

Spoon model for each project is cached after the first run (in the Launcher/models directory). The analyzer **will not** detect any code changes.

## Running as a standalone analyzer
There is no out-of-box support for building "Launcher" as a standalone static analyzer, but you should be able to add an "application" Gradle plugin to make a standalone distribution. 

The analyzer report will be printed to stdout along with the logs so you probably won't be able to use it non-manually.

# Testing
## "Kawa" tests
These unit tests are generally supposed to fail. Their purpose is mostly the evaluation of the analyzer capabilities - for example, checking if the analyzer supports symbolic calculations (it doesn't for now).

## "Launcher" tests
These tests are made for tracking the consistency of the Kawa's calculations and behavior changes. The tests compare the current analyzer report with the "reference" report from the repository and fail if something has changed. The tests are being run on a couple of open source projects for which you have to get the sources by running the "prepareTestProjects" Gradle task (requires git to be present in PATH).

Model cache uses the same Launcher/models directory. Don't forget to clean it up when changing versions of the checked projects.

Project sources are placed in the Launcher/projects directory.

Running the tests creates reports in the Launcher/reports directory (see `*project_name*.latest.json`). If you wish to update a reference report (`*project_name*.reference.json`), replace it with a "latest" one in the same directory.

# Contributing
Please remember that Kawa is not supposed to be a full-blown production-grade static analyzer. Instead, it is a DataFlow module that can be used in real-world analyzers.

That being said, any contributions are welcome!