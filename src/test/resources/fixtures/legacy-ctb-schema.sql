-- Pre-shared-node CTB schema, plus a node whose content/hierarchy must survive.
CREATE TABLE bookmark (node_id INTEGER UNIQUE,sequence INTEGER);
CREATE TABLE children (node_id INTEGER UNIQUE,father_id INTEGER,sequence INTEGER);
CREATE TABLE codebox (node_id INTEGER,offset INTEGER,justification TEXT,txt TEXT,syntax TEXT,width INTEGER,height INTEGER,is_width_pix INTEGER,do_highl_bra INTEGER,do_show_linenum INTEGER);
CREATE TABLE grid (node_id INTEGER,offset INTEGER,justification TEXT,txt TEXT,col_min INTEGER,col_max INTEGER);
CREATE TABLE image (node_id INTEGER,offset INTEGER,justification TEXT,anchor TEXT,png BLOB,filename TEXT,link TEXT,time INTEGER);
CREATE TABLE node (node_id INTEGER UNIQUE,name TEXT,txt TEXT,syntax TEXT,tags TEXT,is_ro INTEGER,is_richtxt INTEGER,has_codebox INTEGER,has_table INTEGER,has_image INTEGER,level INTEGER,ts_creation INTEGER,ts_lastsave INTEGER);
INSERT INTO node VALUES (1,'Legacy çığ','Preserve Türkçe content','plain-text','',0,0,0,0,0,0,0,0);
INSERT INTO children VALUES (1,0,1);
INSERT INTO bookmark VALUES (1,1);
