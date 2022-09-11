CREATE TABLE logs
(
    id        timeuuid PRIMARY KEY,
    timeid    timeuuid,
    message   text,
    level     text,
    marker    text,
    logger    text,
    timestamp date,
    mdc       map<text,text>,
    ndc       list<text>
);