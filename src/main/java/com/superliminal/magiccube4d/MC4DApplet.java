package com.superliminal.magiccube4d;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.io.PushbackReader;
import java.io.StringReader;

import javax.swing.JProgressBar;

import com.superliminal.util.ResourceUtils;


/**
 * Represents a simple Applet demo version of the core MagicCube4D puzzle.
 * 
 * Copyright 2005 - Superliminal Software
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DApplet extends Applet {
    public MC4DApplet() {
    }

    @Override
    public void init() {
        String lengthstr = getParameter("length");
        int length = lengthstr == null ? 3 : Integer.parseInt(lengthstr);
        System.out.println("length = " + length);
        String logfile = getParameter("logfile");
        System.out.println("logfile = " + logfile);
        final History hist = new History(length);
        java.net.URL histurl = ResourceUtils.getResource(logfile);
        if(histurl == null)
            System.out.println("couldn't read history file");
        else
            hist.read(new PushbackReader(new StringReader(ResourceUtils.readFileFromURL(histurl))));
        final MC4DView view = new MC4DView(new PuzzleManager("{4,3,3}", "3", new JProgressBar()), new RotationHandler());
        final MC4DView.ItemCompleteCallback applyToHistory = new MC4DView.ItemCompleteCallback() {
            @Override
            public void onItemComplete(MagicCube.TwistData twist) {
                hist.apply(twist);
            }
        };
        view.addStickerListener(new MC4DView.StickerListener() {
            @Override
            public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
                view.animate(twisted, applyToHistory);
            }
        });
        setLayout(new BorderLayout());
        add("Center", view);
    }
}
