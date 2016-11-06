DROP TABLE objekty CASCADE CONSTRAINTS;
DROP TABLE obrazky CASCADE CONSTRAINTS;
DROP TABLE majitele CASCADE CONSTRAINTS;
DROP TABLE sektor CASCADE CONSTRAINTS;

--==========================================
-- objekty
--==========================================
DROP SEQUENCE objekty_seq;
CREATE SEQUENCE objekty_seq START WITH 100 INCREMENT BY 1;

CREATE TABLE objekty (
    id NUMBER NOT null,
    nazev VARCHAR(32) NOT null,
    typ VARCHAR(32),
    editable VARCHAR(1),
    popis VARCHAR(250),
    majitel NUMBER NOT null,
    sektor NUMBER NOT null,
    geometrie SDO_GEOMETRY,
    intervalMajitele WM_PERIOD,
    existence WM_PERIOD,
    rekonstrukce WM_PERIOD
    
    CONSTRAINT pk_objekt PRIMARY KEY (id)
);
-- nazvy tabulky a sloupce musi byt velkymi pismeny
DELETE FROM USER_SDO_GEOM_METADATA WHERE
    TABLE_NAME = 'OBJEKTY' AND COLUMN_NAME = 'GEOMETRIE';

INSERT INTO USER_SDO_GEOM_METADATA VALUES (
    'objekty', 'geometrie',
    -- souradnice X,Y s hodnotami 0-300 a presnosti 1 bod
    SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', 0, 300, 1), SDO_DIM_ELEMENT('Y', 0, 300, 1)),
    -- lokalni (negeograficky) souradnicovy system (v analytickych fcich neuvadet jednotky)
    NULL
);

CREATE INDEX objekt_geometrie_sidx ON objekty(geometrie) indextype is MDSYS.SPATIAL_INDEX;

COMMIT;

-- kontrola validity (na zacatku "valid" muze byt cislo chyby, vizte http://www.ora-code.com/)
-- s udanim presnosti
SELECT nazev, SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geometrie, 1) valid -- 1=presnost
FROM objekty;
-- bez udani presnosti (presne dle nastaveni v metadatech)
SELECT o.nazev, o.geometrie.ST_IsValid()
FROM objekty o;

--==========================================
-- obrazky
--==========================================
DROP SEQUENCE obrazky_seq;
CREATE SEQUENCE obrazky_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE obrazky (
    id NUMBER NOT null,
    objekt NUMBER NOT null,
    img ORDSYS.ORDImage,
    img_si ORDSYS.SI_StillImage,
    img_ac ORDSYS.SI_AverageColor,
    img_ch ORDSYS.SI_ColorHistogram,
    img_pc ORDSYS.SI_PositionalColor,
    img_tx ORDSYS.SI_Texture,
    CONSTRAINT pk_obrazky PRIMARY KEY (id)
);

COMMIT;
--==========================================
-- majitele
--==========================================
DROP SEQUENCE majitele_seq;
CREATE SEQUENCE majitele_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE majitele (
    id NUMBER NOT null,
    jmeno VARCHAR(80) NOT null,
    adresa VARCHAR(80) NOT null,
    CONSTRAINT pk_majitel PRIMARY KEY (id)
);
--==========================================
-- sektor
--==========================================
DROP SEQUENCE sektor_seq;
CREATE SEQUENCE sektor_seq START WITH 10 INCREMENT BY 1;

CREATE TABLE sektor (
    id NUMBER NOT null,
    nazev VARCHAR(32) NOT null,
    geometrie SDO_GEOMETRY,
    CONSTRAINT pk_sektor PRIMARY KEY (id)
);

-- nazvy tabulky a sloupce musi byt velkymi pismeny
DELETE FROM USER_SDO_GEOM_METADATA WHERE
    TABLE_NAME = 'SEKTOR' AND COLUMN_NAME = 'GEOMETRIE';

INSERT INTO USER_SDO_GEOM_METADATA VALUES (
    'sektor', 'geometrie',
    -- souradnice X,Y s hodnotami 0-300 a presnosti 1 bod
    SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', 0, 300, 1), SDO_DIM_ELEMENT('Y', 0, 300, 1)),
    -- lokalni (negeograficky) souradnicovy system (v analytickych fcich neuvadet jednotky)
    NULL
);

DROP INDEX objekt_geometrie_sidx;
CREATE INDEX sektor_geometrie_sidx ON sektor(geometrie) indextype is MDSYS.SPATIAL_INDEX;

COMMIT;

-- kontrola validity (na zacatku "valid" muze byt cislo chyby, vizte http://www.ora-code.com/)
-- s udanim presnosti
SELECT nazev, SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geometrie, 1) valid -- 1=presnost
FROM sektor;
-- bez udani presnosti (presne dle nastaveni v metadatech)
SELECT o.nazev, o.geometrie.ST_IsValid()
FROM objekty o;
--==========================================
-- vytvoreni cizich klicu
--==========================================
ALTER TABLE OBJEKTY 
ADD CONSTRAINT fk_objekt_majitel 
FOREIGN KEY (majitel) 
REFERENCES MAJITELE(id) on delete cascade;

ALTER TABLE OBJEKTY
ADD CONSTRAINT fk_objekt_sektor
FOREIGN KEY (sektor) 
REFERENCES sektor(id) on delete cascade;

ALTER TABLE OBRAZKY
ADD CONSTRAINT fk_obr_objekt
FOREIGN KEY (objekt)
REFERENCES objekty(id) on delete cascade;
