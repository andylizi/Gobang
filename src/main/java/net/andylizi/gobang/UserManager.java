/*
 * Copyright (C) 2016 andylizi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.andylizi.gobang;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpSession;

public class UserManager {

    private static final File FILE_SYSTEM;

    static {
        if (System.getenv("VCAP_SERVICES") != null) {
            Pattern pattern = Pattern.compile("(\"host_path\":\\s?\")(.+?)(\")", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(System.getenv("VCAP_SERVICES"));
            matcher.find();
            FILE_SYSTEM = new File(matcher.group(2));
        } else {
            FILE_SYSTEM = new File("../data/Gobang/");
        }
    }

    public static String login(HttpSession session, String name, String password) {
        if (name == null) {
            return "need_username";
        } else if (password == null) {
            return "need_password";
        }
        File file = new File(FILE_SYSTEM,"users/" + name + ".dat");
        if (!file.exists()) {
            return "un_notfound";
        }
        try {
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                if (password.equals(in.readUTF())) {
                    session.setAttribute("username", name);
                    return "ok";
                } else {
                    return "pw_wrong";
                }
            }
        } catch (FileNotFoundException ex) {
            return "un_notfound";
        } catch (Throwable t) {
            return t.toString();
        }
    }

    public static String register(HttpSession session, String name, String password) {
        if (name == null) {
            return "need_username";
        } else if (password == null) {
            return "need_password";
        }
        if (!name.matches("[\\u4e00-\\u9fa5A-za-z0-9]+")) {
            return "invalid_un";
        }
        if (name.equalsIgnoreCase("Anonymous")) {
            return "exists";
        }
        File file = new File(FILE_SYSTEM,"users/" + name + ".dat");
        if (file.exists()) {
            return "exists";
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
                out.writeUTF(password);
                session.setAttribute("username", name);
                return "ok";
            }
        } catch (FileNotFoundException ex) {
            return ex.toString();
        } catch (Throwable t) {
            return t.toString();
        }
    }
}
