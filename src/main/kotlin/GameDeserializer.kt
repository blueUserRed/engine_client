import java.io.DataInputStream

abstract class GameDeserializer {
    abstract fun deserialize(input: DataInputStream, client: Client): MutableList<Entity>?
    companion object {
        internal fun registerDeserializers(client: Client) {
            client.addEntityDeserializer(PolygonEntity.identifier, PolygonEntity::deserialize)
        }
    }
}

class MainGameDeserializer : GameDeserializer() {

    override fun deserialize(input: DataInputStream, client: Client): MutableList<Entity>? {
        val ents = mutableListOf<Entity>()
        val numEnts = input.readInt()
        for (i in 0 until numEnts) {
            val id = input.readInt()
            val deserializer = client.getEntityDeserializer(id)
            if (deserializer == null) {
                Conf.logger.warning("Couldn't deserialize Entity with unknown id '$id'")
                return null
            }
            val ent = deserializer(input)
            if (ent == null) {
                Conf.logger.warning("Couldn't deserialize Entity with id '$id' because of malformed message!")
                return null
            }
            ents.add(ent)
        }
        return ents
    }

}