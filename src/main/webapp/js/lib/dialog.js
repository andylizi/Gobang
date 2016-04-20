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
    init: function (close_callback, back_callback) {
        $(function () {
            if ($("#mask").length <= 0) {
                $(document.body).append('<div id="mask" style="display:none;opacity:0;"></div>');
            }
            if ($("#mask #dialog").length <= 0) {
                $("#mask").append('<div id="dialog" style="margin-top:50px;opacity:0;"><div id="dialog_title"></div><div id="dialog_content"></div><div id="dialog_actions"></div></div>');
            }
        });
        this.dialogQueue = new Array();
        this.toastQueue = new Array();
        this.dialogOpened = false;
        this.close_callback = close_callback;
        this.back_callback = back_callback;
        String.prototype.calculateWidth = function (fontSize) {
            var e = $("<span id='__tmp" + parseInt(Math.random() * 1000) + "__'></span>")
                    .css({
                        visibility: "hidden",
                        whiteSpace: "nowarp",
                        padding: "0"
                    }).text(this);
            if (fontSize)
                e.css("font-size", fontSize);
            try {
                $(document.body).append(e);
                var width = e[0].offsetWidth;
            } catch (e) {
            }
            e.remove();
            return width;
        };
    },
    _open_: function (title, content, actions, callback, opt) {
        var self = this;
        if(opt){
            setTimeout(function(){
                self._open_(title,content,actions,callback);
            },500);
            return;
        }
        console.log("Open dialog - " + title);
        this.dialogOpened = true;
        $("#dialog_title").html(title);
        $("#dialog_content").html(content);
        if (actions) {
            $("#dialog_actions").html(actions);
            if (this.close_callback && typeof this.close_callback == "function") {
                $("#btn_close").click(this.close_callback);
            }
            if (this.back_callback && typeof this.close_callback == "function") {
                $("#btn_back").click(this.back_callback);
            }
        } else {
            $("#dialog_actions").empty();
        }
        $("#mask").css("display", "block");
        setTimeout(function () {
            $("#mask").css("opacity", "1");
            $("#dialog").css({
                opacity: 1
            });
            setTimeout(function () {
                $("#dialog").css({
                    marginTop: "150px"
                });
                if (callback) 
                    if (typeof callback == "function")
                        return callback();
                    else if (typeof callback == "string")
                        return eval(callback);
            }, 5);
        }, 5);
    },
    openDialog: function (title, content, actions, callback, top) {
        if (this.dialogOpened) {
            if (!top) {
                console.log("Buffer: dialog - " + title);
                this.dialogQueue.push({title: title, content: content, actions: actions, callback: callback});
                return;
            } else {
                this.__param_title_ = title;
                this.__param_content_ = content;
                this.__param_actions = actions;
                this.__param_callback = callback;
                return this.closeDialog("this._open_(this.__param_title_,this.__param_content_,this.__param_actions,this.__param_callback,true);", false);
            }
        }
        return this._open_(title, content, actions, callback);
    },
    closeDialog: function (callback, next) {
        var self = this;
        console.log("Close dialog");
        self.dialogOpened = false;
        $("#mask").css("opacity", 0);
        setTimeout(function () {
            $("#mask").css("display", "none");
        }, 450);
        $("#dialog").css({
            marginTop: "50px",
            opacity: 0
        });
        if (next || (typeof next) == "undefined") {
            setTimeout(function () {
                if (self.dialogQueue.length != 0) {
                    var task = self.dialogQueue.shift();
                    self.openDialog(task.title, task.content, task.actions, task.callback);
                    return;
                }
            }, 500);
        }
        if (callback) 
            if (typeof callback == "function")
                return callback();
            else if (typeof callback == "string")
                return eval(callback);
    },
    makeToast: function (content, time, e) {
        if ($(".toast").length > 0) {
            if (!e) {
                this.toastQueue.push({content: content, time: time});
                return false;
            }
            $(".toast").remove();
        }
        var self = this;
        var toast = $('<div class="toast">' + content + '</div>');
        toast.appendTo($(document.body)).css("width", (content.calculateWidth() + 64) + "px");
        setTimeout(function () {
            toast.remove();
            if (self.toastQueue.length > 0) {
                var task = self.toastQueue.shift();
                self.makeToast(task.content, task.time);
            }
        }, !time ? 5000 : time);
        return toast;
    },
    isOpen: function () {
        return this.dialogOpened;
    },
    onCloseButton: function (close_callback) {
        this.close_callback = typeof close_callback == "function" ? close_callback : null;
    },
    onBackButton: function (back_callback) {
        this.back_callback = typeof back_callback == "function" ? back_callback : null;
    }
});