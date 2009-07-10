
M2=$(HOME)/.m2/repository
mmbase=1.9-SNAPSHOT
MM2=$(M2)/org/mmbase/


export CLASSPATH = target/classes:$(MM2)/mmbase/$(mmbase)/mmbase-$(mmbase).jar:$(MM2)/mmbase-media/$(mmbase)/mmbase-media-$(mmbase)-classes.jar:$(MM2)/mmbase-streams/$(mmbase)/mmbase-streams-$(mmbase)-classes.jar:$(MM2)/mmbase-rmmci/$(mmbase)/mmbase-rmmci-$(mmbase)-client.jar




.PHONY:
import:
	java eu.openimages.AssetImporter /share/oip/test/files/BG
