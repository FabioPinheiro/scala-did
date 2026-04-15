# Documentation Guide: How to Write and Improve Docs

This guide outlines the philosophy and process for creating and maintaining documentation in this project. Documentation here serves two audiences: human developers and AI agents.

## Core Philosophy
*   **Clarity over Depth**: Keep sentences clear and simple. Avoid deep dives unless absolutely necessary.
*   **Structure & Maintainability**: Keep documentation organized, clear, and highly maintainable. Link between files instead of repeating content.
*   **Progressive Disclosure**: Only document what is *not* self-evident from reading a single source file. Focus on non-obvious patterns and gotchas.
*   **Audience Awareness**: Write for both humans (quick context) and AI agents (structured, factual information, using clear headings and lists).

## Documentation Pipeline (How it Works)
The documentation generation relies on a multi-step process:

1.  **Source Code**: Code in various modules (e.g., `did-method-prism`) contains ScalaDoc comments.
2.  **Generation**: The `sbt` commands (`sbt docAll` or `sbt docs/unidoc`) process these comments into API reference pages.
3.  **Website Build**: The `sbt siteAll` command uses Laika to consume these generated API docs and structure them into a navigable website.
4.  **Post-Edit Build**: After editing any executable code example within documentation files, the pipeline must be manually triggered by running `sbt siteAll` to regenerate the documentation site and make sure there is no errors.
5.  **Custom Guides**: Files like this guide (`DOCUMENTATION_GUIDE.md`) are manually placed in the `docs/` directory and are *not* part of the automated ScalaDoc pipeline; they are static guides.

## Writing Guidelines
*   **Be Factual**: Only document what is observed in the codebase; never invent commands or conventions.
*   **Link, Don't Repeat**: If a concept is detailed in `didcomm-protocols.md`, link to it instead of re-explaining it in a general guide.
*   **Focus on "Why"**: When explaining a pattern, explicitly state the underlying reason (e.g., "Due to dependency conflict X, we use merge strategy Y in `build.sbt`"). This context is critical for AI agents to grasp design intent.
*   **When in Doubt**: If a concept is unclear or requires deep domain knowledge, note it, and raise a clarification question rather than making an assumption.

## NOTES

* **Linking** - Use markdown links (`[Link Text](path/to/file.md)`) to connect concepts between guides and reference files. This keeps the documentation clean and non-repetitive.
* **Naming Convention**: Use lowercase-kebab-case for new files in `docs/src/` directories.