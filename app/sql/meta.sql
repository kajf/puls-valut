DROP TABLE IF EXISTS ANDROID_METADATA;
DROP TABLE IF EXISTS T_PROVIDER_RATES;
DROP TABLE IF EXISTS T_RATES_PLACES;
DROP TABLE IF EXISTS T_BEST_RATES;

create table ANDROID_METADATA (locale text default 'ru_RU');

insert into ANDROID_METADATA values ('ru_RU');

create table T_PROVIDER_RATES (
			PROVIDER_RATE_ID integer NOT NULL primary key autoincrement,
			PROVIDER_ID integer not null,
			EXCHANGE_TYPE_ID integer not null,
			RATE_TYPE_ID integer not null,
			CURRENCY_ID integer not null,
			VALUE real null,
			TIME_UPDATED integer not null,
			TIME_EFFECTIVE integer not null default 0 );

create table T_RATES_PLACES (
			RATES_PLACE_ID integer NOT NULL primary key, 
			DESCRIPTION text not null, 
			REGION_ID integer DEFAULT 0 NOT NULL,
			BANK_ID integer null, 
			X real null, 
			Y real null, 
			ADDR text null, 
			WORK_HOURS text null,
			PHONE text null);

create table T_BEST_RATES (
			BEST_RATES_ID integer NOT NULL primary key autoincrement,
			CURRENCY_ID integer not null, 
			EXCHANGE_TYPE_ID integer not null,
			RATES_PLACE_ID integer not null,
			TIME_UPDATED integer not null,
			VALUE real not null);			