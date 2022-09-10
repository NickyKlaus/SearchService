CREATE TABLE auth.logs
(
    id        timeuuid PRIMARY KEY,
    timeid    timeuuid,
    message   text,
    level     text,
    marker    text,
    logger    text,
    timestamp timestamp,
    mdc       map<text,text>,
    ndc       list<text>
);