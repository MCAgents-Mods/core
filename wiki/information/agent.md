---
name: wiki-information-agent
description: How core's agent answers a prompt — the three model steps and tool rounds — with the calls and the player's settings on the API pages.
---

# The agent

The agent answers a prompt with the registered tools a prompt needs, through core's
model client. It never sends the model every tool at once: it narrows them down in three
steps, each one request.

This page says how it works. The calls are on the API pages:
[asking the agent](../api/agent.md) and [the player's settings](../api/settings.md).

## The three steps

| Step | The model is sent | It answers with |
|---|---|---|
| 1. Categories | The prompt, and every category's id and description | `select_categories`: one or more category ids |
| 2. Tools | The prompt, and the name and description of each tool in the picked categories | `select_tools`: the tool names it needs |
| 3. Answer | The conversation, and the full definition — name, description and argument schema — of each picked tool | Tool calls, then the answer |

* Step 1 is skipped when there is only one category to pick from.
* With no tools registered, or none picked, step 3 offers no tools and the question costs one
  request.
* A model that answers step 1 or 2 in prose instead of the function is read for the names it
  mentions. Only names that were offered are kept.
* In steps 1 and 2 the list is the system message and the prompt is the user message, so the
  part that stays the same between questions comes first, where a provider can cache it.

## Tool rounds

In step 3 the model may call tools, read their results, and call again. A **round** is one
batch of calls and their results. The player sets how many rounds a question may take, and a
request may set its own instead. After the last round the model is told to answer from what it
has; if it asks for tools again anyway, its text so far is the answer, or a note that it did
not reach one.

The calls in a round run on the worker pool as [tools.md](tools.md) describes: `CONCURRENT`
calls together, `WAIT` calls alone, results in the order asked. A call to a tool the model
was not offered in step 3 is answered with an error and not run.

The registry is read once per question. A tool removed while a question is in progress
still answers that question.
