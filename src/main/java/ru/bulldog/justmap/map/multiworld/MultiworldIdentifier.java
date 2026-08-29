package ru.bulldog.justmap.map.multiworld;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import ru.bulldog.justmap.util.PosUtil;

public class MultiworldIdentifier {
	public final BlockPos spawnPosition;
	public final Identifier dimensionType;

	public MultiworldIdentifier(BlockPos spawnPosition, Identifier dimensionType) {
		this.spawnPosition = spawnPosition;
		this.dimensionType = dimensionType;
	}

	public MultiworldIdentifier(BlockPos spawnPosition, Level world) {
		this(spawnPosition, world.dimension().identifier());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MultiworldIdentifier that = (MultiworldIdentifier) o;

		if (!spawnPosition.equals(that.spawnPosition)) return false;
		return dimensionType.equals(that.dimensionType);
	}

	@Override
	public int hashCode() {
		int result = spawnPosition.hashCode();
		result = 31 * result + dimensionType.hashCode();
		return result;
	}

	public JsonObject toJson() {
		JsonObject jsonKey = new JsonObject();
		jsonKey.addProperty("dimension", dimensionType.toString());
		jsonKey.add("position", PosUtil.toJson(spawnPosition));
		return jsonKey;
	}

	public static MultiworldIdentifier fromJson(JsonObject object) {
		Identifier dimensionType = Identifier.parse(GsonHelper.getAsString(object, "dimension"));
		BlockPos spawnPosition = PosUtil.fromJson(GsonHelper.getAsJsonObject(object, "position"));
		return new MultiworldIdentifier(spawnPosition, dimensionType);
	}
}
