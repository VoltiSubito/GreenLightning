package com.ociweb.gl.example;

import com.ociweb.pronghorn.network.config.HTTPHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPFieldReader;
import com.ociweb.gl.api.HTTPRequestReader;
import com.ociweb.gl.api.Headable;
import com.ociweb.gl.api.MsgCommandChannel;
import com.ociweb.gl.api.NetResponseTemplate;
import com.ociweb.gl.api.NetResponseTemplateData;
import com.ociweb.gl.api.NetResponseWriter;
import com.ociweb.gl.api.Writable;
import com.ociweb.gl.api.RestListener;
import com.ociweb.gl.impl.HTTPPayloadReader;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;
import com.ociweb.pronghorn.network.config.HTTPHeaderDefaults;
import com.ociweb.pronghorn.network.schema.HTTPRequestSchema;
import com.ociweb.pronghorn.pipe.ChannelReader;
import com.ociweb.pronghorn.pipe.ChannelWriter;
import com.ociweb.pronghorn.util.Appendables;
import com.ociweb.pronghorn.util.math.Decimal;
import com.ociweb.pronghorn.util.math.DecimalResult;

public class MathUnit implements RestListener {

	private final Logger logger = LoggerFactory.getLogger(MathUnit.class);
	
	private final MsgCommandChannel<?> cc;
	private String lastCookie;
	private final byte[] fieldA = "a".getBytes();
	private final byte[] fieldB = "b".getBytes();
	
	private final NetResponseTemplate<HTTPFieldReader> template;

	public MathUnit(final GreenRuntime runtime) {

		this.cc = runtime.newCommandChannel(MsgCommandChannel.NET_RESPONDER);
		
		NetResponseTemplateData<HTTPFieldReader> consumeX = new NetResponseTemplateData<HTTPFieldReader>() {

			@Override
			public void fetch(ChannelWriter writer, HTTPFieldReader source) {
				source.getText(fieldA, writer);
			}
			
		};
		
		NetResponseTemplateData<HTTPFieldReader> consumeY = new NetResponseTemplateData<HTTPFieldReader>() {

			@Override
			public void fetch(ChannelWriter writer, HTTPFieldReader source) {
				source.getText(fieldB, writer);
			}
			
		};
		
		NetResponseTemplateData<HTTPFieldReader> consumeSum = new NetResponseTemplateData<HTTPFieldReader>() {

			@Override
			public void fetch(final ChannelWriter writer, HTTPFieldReader source) {
				 DecimalResult adder = new DecimalResult() {
						@Override
						public void result(long m, byte e) {
							Appendables.appendDecimalValue(writer, m, e);
						}				    		 
			    	 };
					Decimal.sum(
							source.getDecimalMantissaDirect(fieldA),
							source.getDecimalExponentDirect(fieldA), 
							source.getDecimalMantissaDirect(fieldB),
							source.getDecimalExponentDirect(fieldB),
			    			 adder);		
			}
			
		};
		
		template = new NetResponseTemplate<HTTPFieldReader>()
				     .add("{\"x\":").add(consumeX)
				     .add(",\"y\":").add(consumeY)
				     .add(",\"groovySum\":").add(consumeSum)
				     .add("}");		
	}
	
	
	
	@Override
	public boolean restRequest(final HTTPRequestReader request) {
		
		final StringBuilder cookieValue = new StringBuilder();
		Headable eat = new Headable() {

			@Override
			public void read(HTTPHeader header, ChannelReader httpPayloadReader) {
				httpPayloadReader.readUTF(cookieValue);
				lastCookie = cookieValue.toString();
			}
			
		};
		request.openHeaderData(HTTPHeaderDefaults.COOKIE.rootBytes(),eat);
				
		Writable render = new Writable() {

			@Override
			public void write(ChannelWriter writer) {
					template.render(writer, request);
			}
			
		};
		return cc.publishHTTPResponse(request.getConnectionId(), request.getSequenceCode(), 200, false,
				                   HTTPContentTypeDefaults.JSON, render );		

	}

	public String getLastCookie() {
		return lastCookie;
	}	

}
