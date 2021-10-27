FuzzPEG: A Context-Free Random Program Generator
================================================

*FuzzPEG* is a random program generator. It takes as input a context-free grammar (to be more
precise, a so-called Parsing Expression Grammar, PEG) and efficiently generates syntactically valid
programs for it. These programs can then be used to detect bugs in compilers and other language
processors.

To make *FuzzPEG* usable for practical purposes, it offers some advanced features:

- *FuzzPEG* not only tracks which parts of the input grammar have already been covered by previously
  generated programs, but can also guide the generation process towards uncovered parts of the
  grammar. This increases the diversity of the generated programs.
- To give the user more control over the size and shape of the generated programs, *FuzzPEG*
  supports probabilistic grammars. Such grammars include weights that determine the probability that
  certain alternatives are chosen.
- To avoid the construction of "infinitely" large programs in case of recursive grammars, *FuzzPEG*
  uses a height limit and only chooses alternatives that fit into this limit. *FuzzPEG*
  automatically chooses a height limit that allows to cover all parts of the input grammar (but the
  height limit can also be specified manually via a command line option).
- *FuzzPEG* ensures that the serialized programs are lexically valid (this is a necessary
  precondition for being syntactically valid):
  - When *FuzzPEG* generates a random token (e.g., an identifier), it makes sure that the randomly
    generated string does not collide with a different terminal (e.g., a keyword).
  - Simply concatenating two adjacent tokens may lead to an invalid result (e.g., if two adjacent
    identifiers "foo" and "bar" are concatenated, the resulting "foobar" no longer consists of two
    identifiers). Thus, *FuzzPEG* automatically determines if two adjacent tokens require a
    separator; by default, this separator consists of a single space (but the separator may also be
    specified via a command line option).

Although performance is not its top-most priority, *FuzzPEG* is reasonably efficient. On a standard
desktop computer and for a typical real-world grammar, *FuzzPEG* should be able to generate several
hundred KiB of syntactically valid output per second.


## Building FuzzPEG

*FuzzPEG* uses the [j-PEG](https://github.com/FAU-Inf2/j-PEG) library for parsing and analyzing the
input grammar, which is included as a Git submodule in `libs/j-PEG`. Use one of the following ways
to correctly set up this submodule:

- When cloning the *FuzzPEG* repository, add the command line option `--recurse-submodules`.
- If you already cloned the *FuzzPEG* repository without the aforementioned command line option,
  simply type `git submodule update --init` to set up the submodule.

When the submodule is correctly set up, simply type `./gradlew build` in the root directory of the
*FuzzPEG* repository to build the *FuzzPEG* framework. After a successful build, there should be a
file `build/libs/FuzzPEG.jar`. The instructions below assume that this file exists.

**Note**:

- You need a working JDK installation to build and run *FuzzPEG* (tested with OpenJDK 8 and 11).
- Building *FuzzPEG* requires an internet connection to resolve external dependencies.


## License

*FuzzPEG* is licensed under the terms of the MIT license (see [LICENSE.mit](LICENSE.mit)).

*FuzzPEG* makes use of the following open-source projects:

- Gradle (licensed under the terms of Apache License 2.0)
