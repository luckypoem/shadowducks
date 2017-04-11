package com.github.netfreer.shadowducks.common.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: landy
 * @date: 2017-04-11 20:26
 */
public class TransferHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TransferHandler.class);
    private final Channel output;
    private ChannelHandlerContext localCtx;
    private ChannelFutureListener continueReadOrCloseFuture = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
                future.channel().pipeline().fireExceptionCaught(future.cause());
            } else {
                localCtx.read();
            }
        }
    };

    public TransferHandler(Channel output) {
        this.output = output;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ChannelOutboundBuffer outboundBuffer = ctx.channel().unsafe().outboundBuffer();
        if (outboundBuffer.totalPendingWriteBytes() > 0) {
            LOG.debug("any data need to flush");
            ctx.flush();
        }
        ctx.read();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.localCtx = ctx;
        super.handlerAdded(ctx);
        if (ctx.channel().isActive()) {
            ctx.read();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        output.write(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        output.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(continueReadOrCloseFuture);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (output.isOpen()) {
            output.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.warn("data forward transfer exception", cause);
        ctx.close();
    }
}
