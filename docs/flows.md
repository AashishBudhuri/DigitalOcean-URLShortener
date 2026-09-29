# URL Shortener — Request Flows

## Create

User submits a long URL with an optional alias. If no alias is provided, one is generated. If a provided alias already exists, the request fails; otherwise a DB entry is created.

```mermaid
flowchart TD
    A[User submits long URL<br/>+ optional alias] --> B{Alias provided?}
    B -->|No| C[Generate unique alias]
    B -->|Yes| D{Alias already exists?}
    C --> E[Create DB entry:<br/>long URL + alias]
    D -->|Yes| F[Return error:<br/>alias already taken]
    D -->|No| E
    E --> G[Return success<br/>with short URL]
```

## Resolve

User submits a short URL. Lookup hits cache first, then DB on miss. On a DB hit, the mapping is written through to cache before redirecting.

```mermaid
flowchart TD
    A[User submits short URL] --> B[Look up alias in cache]
    B --> C{Found in cache?}
    C -->|Yes| D[Redirect to long URL]
    C -->|No| E[Look up alias in DB]
    E --> F{Found in DB?}
    F -->|Yes| G[Store alias → long URL<br/>in cache]
    G --> D
    F -->|No| H[Return not found error]
```

## Request lifecycle and data flow

```mermaid
flowchart LR
    subgraph Clients
        U[User / Client]
    end

    subgraph API["API Layer"]
        R[Request Router]
        C[Create Handler]
        S[Resolve Handler]
    end

    subgraph Data["Data Layer"]
        Cache[(Cache)]
        DB[(Database)]
    end

    U -->|1. POST long URL + optional alias| R
    R --> C
    C -->|check alias| DB
    DB -->|exists| C
    C -->|error: alias taken| U
    DB -->|missing| C
    C -->|insert long URL + alias| DB
    C -->|success + short URL| U

    U -->|2. GET short URL| R
    R --> S
    S -->|lookup alias| Cache
    Cache -->|hit: long URL| S
    S -->|miss| DB
    DB -->|found: long URL| S
    S -->|write-through| Cache
    S -->|302 redirect to long URL| U
    DB -->|not found| S
    S -->|404| U
```

| Path | Steps | Data touched |
|------|--------|----------------|
| **Create** | Request → Alias check → Insert → Response | DB write; no cache |
| **Resolve** | Request → Cache → (DB on miss) → Cache fill → Redirect | Cache read/write; DB read |
