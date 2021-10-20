import java.io.DataInputStream
import java.util.*

abstract class GameDeserializer {

    abstract fun deserialize(input: DataInputStream, client: Client): MutableList<Entity>?
    abstract fun deserializeInc(input: DataInputStream, client: Client): Boolean

    companion object {
        internal fun registerDeserializers(client: Client) {
            client.addEntityDeserializer(PolygonEntity.identifier) { PolygonEntity.deserialize(it, client) }
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

    override fun deserializeInc(input: DataInputStream, client: Client): Boolean {
        var identifier = input.readInt()
        while (identifier != Int.MIN_VALUE) {
            val isNew = input.readBoolean()
            if (isNew) {
                val deserializer = client.getEntityDeserializer(identifier) ?: run {
                    Conf.logger.warning("Couldn't deserialize Entity with unknown identifier '$identifier'")
                    return false
                }
                client.addEntity(deserializer(input) ?: run {
                    Conf.logger.warning("Couldnt deserialize Enitity with identifier '$identifier'")
                    return false
                })
                identifier = input.readInt()
                continue
            }
            val uuid = UUID(input.readLong(), input.readLong())
            val ent = client.getEntity(uuid)
            if (ent == null) {
                Conf.logger.warning("Couldn't deserialize incremental message because " +
                        "there is no entity with uuid '${uuid}'")
                return false
            }
            ent.deserializeInc(input, client)
            identifier = input.readInt()
        }
        return true
    }

}