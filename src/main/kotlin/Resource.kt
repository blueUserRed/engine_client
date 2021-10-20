import javafx.scene.image.Image

interface Resource

@JvmInline
value class ImageResource(val image: Image) : Resource