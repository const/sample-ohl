-- public.company definition

-- Drop table

-- DROP TABLE public.company;

CREATE TABLE IF NOT EXISTS public.company (
	company_id uuid NOT NULL,
	description varchar(255) NULL,
	industry varchar(255) NULL,
	"name" varchar(255) NOT NULL,
	url varchar(255) NULL,
	CONSTRAINT company_pkey PRIMARY KEY (company_id)
);
CREATE INDEX IF NOT EXISTS company_by_name_idx ON public.company USING btree (name);


-- public.company_office definition

-- Drop table

-- DROP TABLE public.company_office;

CREATE TABLE IF NOT EXISTS  public.company_office (
	office_id uuid NOT NULL,
	address varchar(255) NOT NULL,
	city varchar(255) NOT NULL,
	"name" varchar(255) NOT NULL,
	company_id uuid NOT NULL,
	CONSTRAINT company_office_pkey PRIMARY KEY (office_id),
	CONSTRAINT fk10gcfd29bxkkjrpab7dt6yh2l FOREIGN KEY (company_id) REFERENCES public.company(company_id)
);
CREATE INDEX IF NOT EXISTS company_office_by_company_id_idx ON public.company_office USING btree (company_id);


-- public.contact_person definition

-- Drop table

-- DROP TABLE public.contact_person;

CREATE TABLE IF NOT EXISTS public.contact_person (
	contact_person_id uuid NOT NULL,
	"name" varchar(255) NOT NULL,
	"position" varchar(255) NULL,
	company_id uuid NOT NULL,
	CONSTRAINT contact_person_pkey PRIMARY KEY (contact_person_id),
	CONSTRAINT fkmiv7gccctc9jo4ojti577ttbl FOREIGN KEY (company_id) REFERENCES public.company(company_id)
);
CREATE INDEX IF NOT EXISTS contact_persons_by_company_id_idx ON public.contact_person USING btree (company_id);


-- public.verification_info definition

-- Drop table

-- DROP TABLE public.verification_info;

CREATE TABLE IF NOT EXISTS public.verification_info (
	"comment" varchar(255) NULL,
	status varchar(255) NOT NULL,
	"timestamp" timestamptz(6) NOT NULL,
	username varchar(255) NOT NULL,
	company_id uuid NOT NULL,
	CONSTRAINT verification_info_pkey PRIMARY KEY (company_id),
	CONSTRAINT verification_info_status_check CHECK (((status)::text = ANY ((ARRAY['VERIFIED'::character varying, 'INVALID'::character varying])::text[]))),
	CONSTRAINT fk2631d1desupjf5fo8mtgd7srv FOREIGN KEY (company_id) REFERENCES public.company(company_id)
);
CREATE INDEX IF NOT EXISTS verification_info_by_company_id_idx ON public.verification_info USING btree (company_id);


-- public.contact_detail definition

-- Drop table

-- DROP TABLE public.contact_detail;

CREATE TABLE IF NOT EXISTS public.contact_detail (
	contact_person_id uuid NOT NULL,
	contact_type varchar(255) NOT NULL,
	value varchar(255) NOT NULL,
	CONSTRAINT contact_detail_contact_type_check CHECK (((contact_type)::text = ANY ((ARRAY['EMAIL'::character varying, 'PHONE'::character varying, 'TELEGRAM'::character varying])::text[]))),
	CONSTRAINT contact_detail_pkey PRIMARY KEY (contact_person_id, contact_type, value),
	CONSTRAINT fkh7yxxcj9rqwapyld8ll4kswqs FOREIGN KEY (contact_person_id) REFERENCES public.contact_person(contact_person_id)
);
CREATE INDEX IF NOT EXISTS contact_detail_by_contact_person_id_idx ON public.contact_detail USING btree (contact_person_id);
CREATE INDEX IF NOT EXISTS contact_detail_by_value_contact_person_id_idx ON public.contact_detail USING btree (value, contact_person_id);

create or replace view verification_info_for_jimmer as
select
    company_id, company_id as company_id_for_jimmer,
    "comment", status, "timestamp", username
from public.verification_info;

create or replace view contact_detail_for_jimmer as
select
    contact_person_id, contact_type, value,
    contact_person_id as contact_person_id_for_jimmer,
    contact_type as contact_type_for_jimmer,
    value as value_for_jimmer
from public.contact_detail;


