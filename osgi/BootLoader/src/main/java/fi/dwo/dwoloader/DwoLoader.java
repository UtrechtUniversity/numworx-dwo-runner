package fi.dwo.dwoloader;

import fi.dwo.bootloader.LoaderBuilder;

public interface DwoLoader {
	LoaderBuilder.Update getUpdate();
}
