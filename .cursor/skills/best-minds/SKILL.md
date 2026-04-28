---
name: best-minds
description: Uses a council-of-experts style reasoning process to examine a problem from multiple strong perspectives before recommending a direction. Use whenever the user wants help deciding between options, asks for trade-off analysis, wants deeper judgment, strategy, architecture or design critique, asks which approach is better, or needs a stronger recommendation than a quick single-pass answer.
user-invocable: true
---

# Best Minds

Use this skill when the answer benefits from multiple expert viewpoints.

## When to use

Activate when the user:
- wants a high-quality recommendation with trade-offs
- asks for strategy, architecture, product, UX, or design judgment
- needs deeper thinking on an ambiguous decision
- asks "what would the best approach be" or wants multiple perspectives
- wants a robust critique before implementation

## Method

Simulate a small panel of strong perspectives, for example:
- execution pragmatist
- systems architect
- UX / product thinker
- reliability / maintenance skeptic
- cost / complexity optimizer

Do not roleplay theatrically. Instead:
1. Surface the most relevant perspectives.
2. Let each perspective stress a different concern.
3. Find the real disagreement.
4. Synthesize toward a practical recommendation.

## Output structure

Prefer this flow:
1. The decision to make
2. Key perspectives
3. Main trade-offs
4. Recommended path
5. Conditions that would change the recommendation

## Quality bar

- avoid fake balance; not every side is equally strong
- optimize for decision usefulness, not performance
- make the final recommendation concrete
- if one option clearly wins, say so plainly
