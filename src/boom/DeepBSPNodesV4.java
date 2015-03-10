package boom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import utils.C2JUtils;
import w.CacheableDoomObject;

public class DeepBSPNodesV4 implements CacheableDoomObject{

	public static final byte[] DeepBSPHeader={
		'x','N','d','4',0,0,0,0
		};
	
	byte[] header=new byte[8];
	mapnode_v4_t[] nodes;
	int numnodes;
	
	public boolean formatOK(){
		return Arrays.equals(header, DeepBSPHeader);
	}

	public mapnode_v4_t[] getNodes(){
		return nodes;
	}
	
	@Override
	public void unpack(ByteBuffer buf) throws IOException {
		int length=buf.capacity();
		
		// Too short, not even header.
		if (length<8) return;
		
		numnodes=(length-8)/mapnode_v4_t.sizeOf();
		
		if (length<1) return;
		
		buf.get(header); // read header
		
		nodes=C2JUtils.createArrayOfObjects(mapnode_v4_t.class, length);
		
		for (int i=0;i<length;i++){
			nodes[i].unpack(buf);
		}
		
	}
	
	
	

}
