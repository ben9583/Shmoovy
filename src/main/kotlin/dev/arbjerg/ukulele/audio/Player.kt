package dev.arbjerg.ukulele.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.Buffer
import java.nio.ByteBuffer


class Player(apm: AudioPlayerManager) : AudioEventAdapter(), AudioSendHandler {
    private val queue = TrackQueue()
    private val player = apm.createPlayer().apply { addListener(this@Player) }
    private val buffer = ByteBuffer.allocate(1024)
    private val frame: MutableAudioFrame = MutableAudioFrame().apply { setBuffer(buffer) }
    private val log: Logger = LoggerFactory.getLogger(Player::class.java)

    /**
     * @return whether or not we started playing
     */
    fun add(vararg tracks: AudioTrack): Boolean {
        queue.add(*tracks)
        if (player.playingTrack == null) {
            player.playTrack(queue.take()!!)
            return true
        }
        return false
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        val new = queue.take() ?: return
        player.playTrack(new)
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        log.error("Track exception", exception)
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        log.error("Track $track got stuck!")
    }

    override fun canProvide(): Boolean {
        return player.provide(frame)
    }

    override fun provide20MsAudio(): ByteBuffer {
        // flip to make it a read buffer
        (buffer as Buffer).flip()
        return buffer
    }

    override fun isOpus() = true
}
