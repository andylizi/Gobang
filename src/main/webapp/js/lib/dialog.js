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
window.Dialog = Class.extend({
    init: function (close_callback, back_callback, buffer_pop_callback) {
        $(function () {
            if ($("#mask").length <= 0) {
                $(document.body).append('<div id="mask" style="display:none;"></div>');
            }
            if ($("#mask #dialog").length <= 0) {
                $("#mask").append('<div id="dialog" style="margin-top:0px;opacity: 0;"><div id="dialog_title"></div><div id="dialog_content"></div><div id="dialog_actions"></div></div>');
            }
        });
        this.dialogQueue = new Array();
        this.toastQueue = new Array();
        this.dialogOpene = false;
        this.close_callback = close_callback;
        this.back_callback = back_callback;
        this.buffer_pop_callback = buffer_pop_callback;
        String.prototype.calculateWidth = function (fontSize) {
            var e = $("<span id='__tmp" + parseInt(Math.random() * 1000) + "__'></span>")
                    .css({
                        visibility: "hidden",
                        whiteSpace: "nowarp",
                        padding: "0"
                    }).text(this);
            if (fontSize)
                e.css("font-size", fontSize);
            try{
                $(document.body).append(e);
                var width = e[0].offsetWidth;
            }catch(e){}
            e.remove();
            return width;
        };
    },
    openDialog: function (title, content, actions, callback) {
        if (this.dialogOpene) {
            console.log("Buffer: dialog - " + title);
            this.dialogQueue.push({title: title, content: content, actions: actions, callback: callback});
            return;
        }
        console.log("Open dialog - " + title);
        this.dialogOpene = true;
        $("#dialog_title").html(title);
        $("#dialog_content").html(content);
        if (actions) {
            $("#dialog_actions").html(actions);
        }
        $("#mask").hide().fadeIn("fast");
        $("#dialog").show().animate({
            "margin-top": "120px",
            "opacity": 1
        }, "fast", function () {
            if (callback) {
                callback();
            }
        });
        if (this.close_callback) {
            $("#btn_close").click(this.close_callback);
        }
        if (this.back_callback) {
            $("#btn_back").click(this.back_callback);
        }
    },
    closeDialog: function (callback) {
        console.log("Hide dialog");
        this.dialogOpene = false;
        if (this.buffer_pop_callback) {
            this.buffer_pop_callback();
        }
        $("#mask").fadeOut("fast");
        var self = this;
        $("#dialog").show().animate({
            "margin-top": "0px",
            "opacity": 0
        }, "fast", "swing", function () {
            $(this).css("display", "none");
            if (callback) {
                callback();
            }
            if (self.dialogQueue.length != 0) {
                var task = self.dialogQueue.shift();
                self.openDialog(task.title, task.content, task.actions, task.callback);
                return;
            }
        });
    },
    makeToast: function (content, time, e) {
        if ($(".toast").length > 0) {
            if (!e) {
                this.toastQueue.push({content: content, time: time, e: e});
                return false;
            }
            $(".toast").remove();
        }
        var self = this;
        var toast = $('<div class="toast">' + content + '</div>');
        toast.appendTo($(document.body)).css("width",(content.calculateWidth()+64)+"px");
        setTimeout(function () {
            toast.remove();
            if (self.toastQueue.length > 0) {
                var task = self.toastQueue.shift();
                self.makeToast(task.content, task.time, task.e);
            }
        }, !time ? 5000 : time);
        return toast;
    },
    isOpen: function () {
        return this.dialogOpene;
    }
});