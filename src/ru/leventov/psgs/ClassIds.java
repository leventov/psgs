package ru.leventov.psgs;

import gnu.trove.function.ObjByteConsumer;
import gnu.trove.function.ToByteFunction;
import gnu.trove.map.*;
import gnu.trove.map.hash.ObjByteDHashMap;

import java.util.Map;

import static ru.leventov.psgs.util.Bits.unsignedByte;

class ClassIds<BASE> {
	private TObjByteMap<Class<? extends BASE>> classIds = new ObjByteDHashMap<>(256);
	private Class[] idClasses = new Class[256];
    private byte nextId = 1;

    private byte getNextId() {
        byte initId = nextId;
        byte id = initId;
        while(id == 0 || idClasses[unsignedByte(id)] != null) {
            id++;
            if (id == initId) {
                throw new RuntimeException("Only 255 types can be indexed.");
            }
        }
        nextId = (byte) (id + 1);
        return id;
    }

    void addClass(Class<? extends BASE> cl, byte id) {
        if (id != 0 && idClasses[unsignedByte(id)] == null) {
            idClasses[unsignedByte(id)] = cl;
            classIds.put(cl, id);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Unsigned id
     */
    void addClass(Class<? extends BASE> cl, int id) {
        if (id != 0 && idClasses[id] == null) {
            idClasses[id] = cl;
            classIds.put(cl, (byte) id);
        } else {
            throw new IllegalArgumentException();
        }
    }

	public Class<? extends BASE> getClass(byte id) {
		return idClasses[unsignedByte(id)];
	}

    /**
     * By unsigned id
     */
    public Class<? extends BASE> getClass(int id) {
        return idClasses[id];
    }

	public byte getId(Class<? extends BASE> cl) {
        return classIds.computeIfAbsent(cl, new ToByteFunction<Class<?>>() {
            @Override
            public byte applyAsByte(Class<?> cl) {
                byte id = getNextId();
                idClasses[unsignedByte(id)] = cl;
                return id;
            }
        });
	}

    public Map<String, Byte> asMap() {
        final Map<String, Byte> map = new ObjByteDHashMap<>();
        classIds.forEach(new ObjByteConsumer<Class<?>>() {
            @Override
            public void accept(Class<?> cl, byte id) {
                map.put(cl.getName(), id);
            }
        });
        return map;
    }

    public void loadMap(Map<String, Byte> map) throws ClassNotFoundException {
        for (Map.Entry<String, Byte> entry : map.entrySet()) {
            byte id = entry.getValue();
            addClass((Class<? extends BASE>) Class.forName(entry.getKey()), id);
        }
    }
}
