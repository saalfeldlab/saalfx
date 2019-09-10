package org.janelia.saalfeldlab.fx.event;

public interface InstallAndRemove<T>
{

	void installInto(T t);

	void removeFrom(T t);

}
