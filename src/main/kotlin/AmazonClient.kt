import AmazonProperties.ACCESS_KEY
import AmazonProperties.IMAGE_ID
import AmazonProperties.SECRET
import org.jclouds.ContextBuilder
import org.jclouds.compute.ComputeServiceContext
import org.jclouds.compute.domain.NodeMetadata
import org.jclouds.ec2.domain.InstanceType
import org.jsoup.Jsoup
import java.io.IOException

class AmazonClient {

    fun run() {
        val context = setupContext()
        val instance = startInstance(context)
        println(downloadFile(instance))
        shutdownInstance(instance, context)
    }

    private fun setupContext(): ComputeServiceContext {
        println("Initializing...")
        return ContextBuilder.newBuilder("aws-ec2")
                .credentials(ACCESS_KEY, SECRET)
                .buildView(ComputeServiceContext::class.java)
    }

    private fun startInstance(context: ComputeServiceContext): NodeMetadata? {
        val template = context.computeService
                .templateBuilder()
                .imageId("eu-west-1/$IMAGE_ID")
                .hardwareId(InstanceType.T2_MICRO)
                .build()

        println("Starting instance of image: ${template.image.name}")

        return try {
            val startTime = System.currentTimeMillis()
            val instance = context
                    .computeService
                    .createNodesInGroup("default", 1, template)
                    .first()
            val endTime = System.currentTimeMillis()

            println("Created instance: ${instance.name} in ${endTime - startTime} ms")
            instance
        } catch (e: Exception) {
            println("Node running error: $e")
            null
        } finally {
            context.close()
        }
    }

    private fun downloadFile(instance: NodeMetadata?): String {
        return try {
            attemptDownload(instance)
        } catch (e: IOException) {
            println("Connection failed: $e")
            Thread.sleep(1000)
            downloadFile(instance)
        }
    }

    private fun attemptDownload(instance: NodeMetadata?): String {
        val address = instance!!.publicAddresses.first()
        val url = "http://" + address
        println("Trying to download a file from: $url")
        return Jsoup.connect(url).get().html()
    }

    private fun shutdownInstance(instance: NodeMetadata?, context: ComputeServiceContext) {
        println("Shutting down instance: ${instance!!.name}")
        context.computeService.destroyNode(instance.id)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val amazonClient = AmazonClient()
            amazonClient.run()
        }
    }
}