package mServer;

import mServer.tool.MServerDaten;
import mServer.tool.MServerLog;
import msearch.Search;

/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
public class Main {

    public Main() {
    }

    private enum StartupMode {

        SERVER, SENDER_LOESCHEN
    }

    public static void main(String[] args) {
        final String ar[] = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                String sender = "";
                String filmDatei = "";
                StartupMode state = StartupMode.SERVER;
                if (ar != null) {
                    for (int i = 0; i < ar.length; ++i) {
                        if (ar[i].equalsIgnoreCase("-d")) {
                            MServerDaten.debug = true;
                        }
                        if (ar[i].equalsIgnoreCase("-v")) {
                            MServerLog.versionsMeldungen(this.getClass().getName());
                            System.exit(0);
                        }
                        if (ar[i].equalsIgnoreCase("-sender")) {
                            state = StartupMode.SENDER_LOESCHEN;
                            if (ar.length > i) {
                                sender = ar[i + 1];
                            }
                        }
                        if (ar[i].equalsIgnoreCase("-filmdatei")) {
                            if (ar.length > i) {
                                filmDatei = ar[i + 1];
                            }
                        }
                    }
                }
                switch (state) {
                    case SERVER:
                        new MServer(ar).starten();
                        break;
                    case SENDER_LOESCHEN:
                        Search.senderLoeschenUndExit(sender, filmDatei);
                        break;
                }
            }
        });

    }
}