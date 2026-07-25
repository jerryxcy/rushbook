# Issue tracker: GitHub

Issues and PRDs for this repository live in GitHub Issues. Use the `gh` CLI
from this repository so it can infer `jerryxcy/rushbook`.

## Conventions

- Create: `gh issue create`
- Read: `gh issue view <number> --comments`
- List: `gh issue list`
- Comment: `gh issue comment <number>`
- Label: `gh issue edit <number> --add-label "<label>"`
- Close: `gh issue close <number>`

## Pull requests as a triage surface

PRs as a request surface: no.

## Skill operations

- “Publish to the issue tracker” means creating a GitHub issue.
- “Fetch the relevant ticket” means reading the issue and its comments.
- Use GitHub native issue dependencies when tickets block one another.
- If native dependencies are unavailable, use a `Blocked by: #<number>` line.
