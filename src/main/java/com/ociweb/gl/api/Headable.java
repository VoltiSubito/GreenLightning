package com.ociweb.gl.api;

import com.ociweb.pronghorn.pipe.BlobReader;

public interface Headable {

	public void read(int headerId, BlobReader reader);

}
