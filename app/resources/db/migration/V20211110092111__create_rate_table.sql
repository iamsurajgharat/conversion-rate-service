Create table conversion_rates(
    id serial primary key,
    source varchar(20) not null,
    target varchar(20) not null,
    from_date date not null,
    to_date date not null,
    value numeric(5,3) not null
);
