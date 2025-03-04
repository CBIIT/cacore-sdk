/*L
   Copyright Ekagra Software Technologies Ltd.
   Copyright SAIC, SAIC-Frederick

   Distributed under the OSI-approved BSD 3-Clause License.
   See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
L*/

-- This script assumes that 'public' schema is used for the particular database in postgres.
-- Structure =
-- > database->schema->tables
-- > database->schema->trigger
-- > database->schema->functions
-- > database->schema->indexes
-- Refer the postgres documentation for further details.
--
-- CREATE SCHEMA public;

SET search_path TO public;

DROP TABLE IF EXISTS LOG_MESSAGE CASCADE;

DROP SEQUENCE  IF EXISTS LOG_MESSAGE_SEQ;

CREATE  SEQUENCE  LOG_MESSAGE_SEQ;

CREATE TABLE LOG_MESSAGE
(
  LOG_ID bigint NOT NULL default nextval('LOG_MESSAGE_SEQ'),
  APPLICATION character varying(25),
  SERVER character varying(50),
  CATEGORY character varying(255),
  THREAD character varying(255),
  USERNAME character varying(255),
  SESSION_ID character varying(255),
  MSG text,
  THROWABLE text,
  NDC text,
  CREATED_ON bigint NOT NULL DEFAULT 0::bigint,
  OBJECT_ID character varying(255),
  OBJECT_NAME character varying(255),
  ORGANIZATION character varying(255),
  OPERATION character varying(50),
  CONSTRAINT LOG_MESSAGE_pkey PRIMARY KEY (LOG_ID)
)
WITH (
  OIDS=FALSE
);
-- ALTER TABLE LOG_MESSAGE OWNER TO postgres;

-- Index: APPLICATION_LOGTAB_INDX

DROP INDEX IF EXISTS APPLICATION_LOGTAB_INDX;

CREATE INDEX APPLICATION_LOGTAB_INDX
  ON LOG_MESSAGE
  USING btree
  (APPLICATION);

-- Index: CREATED_ON_LOGTAB_INDX

DROP INDEX IF EXISTS CREATED_ON_LOGTAB_INDX;

CREATE INDEX CREATED_ON_LOGTAB_INDX
  ON LOG_MESSAGE
  USING btree
  (CREATED_ON);

-- Index: LOGID_LOGTAB_INDX

DROP INDEX IF EXISTS LOGID_LOGTAB_INDX;

CREATE INDEX LOGID_LOGTAB_INDX
  ON LOG_MESSAGE
  USING btree
  (LOG_ID);

-- Index: SERVER_LOGTAB_INDX

DROP INDEX  IF EXISTS SERVER_LOGTAB_INDX;

CREATE INDEX SERVER_LOGTAB_INDX
  ON LOG_MESSAGE
  USING btree
  (SERVER);

-- Index: THREAD_LOGTAB_INDX

DROP INDEX  IF EXISTS THREAD_LOGTAB_INDX;
CREATE INDEX THREAD_LOGTAB_INDX 
  ON LOG_MESSAGE
  USING btree
  (THREAD);

-- Table: OBJECT_ATTRIBUTE

-- DROP TABLE OBJECT_ATTRIBUTE;

DROP TABLE IF EXISTS OBJECT_ATTRIBUTE CASCADE;

DROP SEQUENCE  IF EXISTS OBJECT_ATTRIBUTE_SEQ;

CREATE  SEQUENCE  OBJECT_ATTRIBUTE_SEQ;

CREATE TABLE OBJECT_ATTRIBUTE
(
  OBJECT_ATTRIBUTE_ID bigint NOT NULL default nextval('OBJECT_ATTRIBUTE_SEQ'),
  CURRENT_VALUE character varying(255),
  PREVIOUS_VALUE character varying(255),
  ATTRIBUTE character varying(255) NOT NULL,
  CONSTRAINT OBJECT_ATTRIBUTE_pkey PRIMARY KEY (OBJECT_ATTRIBUTE_ID)
)
WITH (
  OIDS=FALSE
);
-- ALTER TABLE OBJECT_ATTRIBUTE OWNER TO postgres;

-- Index: OAID_INDX

DROP INDEX IF EXISTS OAID_INDX;
CREATE INDEX OAID_INDX
  ON OBJECT_ATTRIBUTE
  USING btree
  (OBJECT_ATTRIBUTE_ID);

-- Table: OBJECTATTRIBUTES

DROP TABLE IF EXISTS OBJECTATTRIBUTES CASCADE;

CREATE TABLE OBJECTATTRIBUTES
(
  LOG_ID bigint NOT NULL DEFAULT 0::bigint,
  OBJECT_ATTRIBUTE_ID bigint NOT NULL DEFAULT 0::bigint
)
WITH (
  OIDS=FALSE
);
-- ALTER TABLE OBJECTATTRIBUTES OWNER TO postgres;

-- Index: FK_OBJECTATTRIBUTES_2

DROP INDEX IF EXISTS FK_OBJECTATTRIBUTES_2;
CREATE INDEX FK_OBJECTATTRIBUTES_2
  ON OBJECTATTRIBUTES
  USING btree
  (OBJECT_ATTRIBUTE_ID);

-- Index: Index_2

DROP INDEX IF EXISTS Index_2;
CREATE INDEX Index_2
  ON OBJECTATTRIBUTES
  USING btree
  (LOG_ID);
