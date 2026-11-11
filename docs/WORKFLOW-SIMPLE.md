# How the app gets built, in plain words

```mermaid
flowchart LR
    A["You hand over a zip:<br/>the designs + what the app should do"] --> B["The Manager reads it<br/>and makes a to-do list"]
    B --> C["A Builder does one<br/>to-do at a time"]
    C --> D["A Checker tests the work<br/>after every few to-dos"]
    D -->|"all good"| E["Work is saved online"]
    D -->|"problem"| F["Builder gets ONE retry"]
    F -->|"fixed"| E
    F -->|"still broken"| G["Noted in a report<br/>for you, and moves on"]
    G --> E
    E -->|"more to-dos left"| C
    E -->|"list finished"| H["Final full test"]
    H --> I["You get a finished app<br/>+ a report of what's left for you"]
```

**Nobody ever stops to ask you.** If something is missing (a password, a key),
the Manager uses a stand-in, writes down what you need to add later, and keeps going.

**If the AI runs out of allowance**, it waits for it to refill and picks up where it stopped.
