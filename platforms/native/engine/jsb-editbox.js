/****************************************************************************
 Copyright (c) 2018 Xiamen Yaji Software Co., Ltd.

 https://www.cocos.com/

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated engine source code (the "Software"), a limited,
  worldwide, royalty-free, non-assignable, revocable and non-exclusive license
 to use Cocos Creator solely to develop games on your target platforms. You shall
  not use Cocos Creator software for developing other software or tools that's
  used for developing games. You are not granted to publish, distribute,
  sublicense, and/or sell copies of Cocos Creator.

 The software or tools in this License Agreement are licensed, not sold.
 Xiamen Yaji Software Co., Ltd. reserves all rights not expressly granted to you.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/

(function () {
    if (!(cc && cc.internal && cc.internal.EditBox)) {
        return;
    }

    const EditBox = cc.internal.EditBox;
    const KeyboardReturnType = EditBox.KeyboardReturnType;
    const InputMode = EditBox.InputMode;
    const InputFlag = EditBox.InputFlag;

    const worldMat = cc.mat4();

    function getInputType (type) {
        switch (type) {
            case InputMode.EMAIL_ADDR:
                return 'email';
            case InputMode.NUMERIC:
            case InputMode.DECIMAL:
                return 'number';
            case InputMode.PHONE_NUMBER:
                return 'phone';
            case InputMode.URL:
                return 'url';
            case InputMode.SINGLE_LINE:
            case InputMode.ANY:
            default:
                return 'text';
        }
    }

    function getKeyboardReturnType (type) {
        switch (type) {
            case KeyboardReturnType.DEFAULT:
            case KeyboardReturnType.DONE:
                return 'done';
            case KeyboardReturnType.SEND:
                return 'send';
            case KeyboardReturnType.SEARCH:
                return 'search';
            case KeyboardReturnType.GO:
                return 'go';
            case KeyboardReturnType.NEXT:
                return 'next';
        }
        return 'done';
    }

    const BaseClass = EditBox._EditBoxImpl;
    class JsbEditBoxImpl extends BaseClass {
        init (delegate) {
            if (!delegate) {
                cc.error('EditBox init failed');
                return;
            }
            this._delegate = delegate;
        }

        beginEditing () {
            const self = this;
            const delegate = this._delegate;
            const multiline = (delegate.inputMode === InputMode.ANY);
            const rect = this._getRect();
            const uvRect = this._getUVRect();
            this.setMaxLength(delegate.maxLength);

            let inputTypeString = getInputType(delegate.inputMode);
            if (delegate.inputFlag === InputFlag.PASSWORD) {
                inputTypeString = 'password';
            }

            function onConfirm (res) {
                delegate._editBoxEditingReturn();
            }

            function onInput (res) {
                if (res.value.length > self._maxLength) {
                    res.value = res.value.slice(0, self._maxLength);
                }

                if (delegate.string !== res.value) {
                    delegate._editBoxTextChanged(res.value);
                }
            }

            function onComplete (res) {
                self.endEditing();
            }

            jsb.inputBox.onInput(onInput);
            jsb.inputBox.onConfirm(onConfirm);
            jsb.inputBox.onComplete(onComplete);

            if (!cc.sys.isMobile) {
                delegate._hideLabels();
            }

            const editLabel = delegate.textLabel;
            // let viewScaleY = cc.view._scaleY;
            // const fontSize = editLabel.fontSize * viewScaleY;
            let viewScaleY = cc.view._scaleY;
            const fontSize = editLabel.fontSize * viewScaleY;
            // const node = this._delegate.node;
            // node.getWorldMatrix(worldMat);
            // console.log("[beginEditing] worldMat : " + worldMat);
            // let viewScaleY = cc.view._scaleY;
            // console.log("[beginEditing] viewScaleY: " + viewScaleY);
            // const dpr = jsb.device.getDevicePixelRatio() || 1;
            // console.log("[beginEditing] dpr: " + dpr);
            // viewScaleY /= dpr;
            // console.log("[beginEditing] worldMat.m05 : " + worldMat.m05);
            // console.log("[beginEditing] viewScaleY2 : " + viewScaleY);
            // const finaleScaleY = viewScaleY / worldMat.m05;
            // console.log("[beginEditing] finaleScaleY : " + finaleScaleY);
            // const fontSize = editLabel.fontSize * finaleScaleY;
            // console.log("[beginEditing] editLabel.fontSize : " + editLabel.fontSize);
            // console.log("[beginEditing] fontSize : " + fontSize);
            let fontPath = "";
            if (editLabel.font != null) {
                let uuid = editLabel.font.uuid;
                fontPath = "assets/main/native/" + uuid.substring(0, 2) + "/" + uuid + "/" + editLabel.font._native;
            }

            jsb.inputBox.show({
                defaultValue: delegate.string,
                maxLength: self._maxLength,
                multiple: multiline,
                confirmHold: false,
                confirmType: getKeyboardReturnType(delegate.returnType),
                inputType: inputTypeString,
                originX: rect.x,
                originY: rect.y,
                width: rect.width,
                height: rect.height,
                uvX: uvRect.uvX,
                uvY: uvRect.uvY,
                uvWidth: uvRect.uvWidth,
                uvHeight: uvRect.uvHeight,
                isBold: editLabel.isBold,
                isItalic: editLabel.isItalic,
                isUnderline: editLabel.isUnderline,
                underlineColor: 0x00000000/* Black */,
                fontPath: fontPath,
                fontSize: /**float */fontSize,
                fontColor: /**number */editLabel.color.toRGBValue(),
                backColor: 0x00ffffff/*White*/,
                backgroundColor: delegate.placeholderLabel.color.toRGBValue(),
                textAlignment: /*left = 0, center = 1, right = 2*/editLabel.horizontalAlign,
            });
            this._editing = true;
            delegate._editBoxEditingDidBegan();
        }

        endEditing () {
            this._editing = false;
            if (!cc.sys.isMobile) {
                this._delegate._showLabels();
            }
            jsb.inputBox.offConfirm();
            jsb.inputBox.offInput();
            jsb.inputBox.offComplete();
            jsb.inputBox.hide();
            this._delegate._editBoxEditingDidEnded();
        }

        setMaxLength (maxLength) {
            if (!isNaN(maxLength)) {
                if (maxLength < 0) {
                    //we can't set Number.MAX_VALUE to input's maxLength property
                    //so we use a magic number here, it should works at most use cases.
                    maxLength = 65535;
                }
                this._maxLength = maxLength;
            }
        }

        _getRect () {
            console.log("==========_getRect==========");
            let canvasSize = cc.view.getCanvasSize();
            console.log("canvasSize: " + canvasSize);
            let visibleSize = cc.view.getVisibleSize();
            console.log("visibleSize: " + visibleSize);
            let visibleSizeInPixel = cc.view.getVisibleSizeInPixel();
            console.log("visibleSizeInPixel: " + visibleSizeInPixel);
            const node = this._delegate.node;
            let viewScaleX = cc.view._scaleX;
            let viewScaleY = cc.view._scaleY;
            const dpr = jsb.device.getDevicePixelRatio() || 1;
            node.getWorldMatrix(worldMat);
            console.log("viewScaleX: " + viewScaleX);
            console.log("viewScaleY: " + viewScaleY);
            console.log("dpr: " + dpr);
            console.log("worldMat: " + worldMat);

            const transform = node._uiProps.uiTransformComp;
            const vec3 = cc.v3();
            let width = 0;
            let height = 0;
            if (transform) {
                const contentSize = transform.contentSize;
                const anchorPoint = transform.anchorPoint;
                width = contentSize.width;
                height = contentSize.height;
                vec3.x = -anchorPoint.x * width;
                vec3.y = -anchorPoint.y * height;

                console.log("contentSize: " + contentSize);
                console.log("anchorPoint: " + anchorPoint);
                console.log("width: " + width);
                console.log("height: " + height);
            }


            const translate = new cc.Mat4();
            console.log("translate: " + translate);
            cc.Mat4.fromTranslation(translate, vec3);
            console.log("translate2: " + translate);
            cc.Mat4.multiply(worldMat, translate, worldMat);
            console.log("worldMat2: " + worldMat);

            viewScaleX /= dpr;
            viewScaleY /= dpr;
            console.log("viewScaleX: " + viewScaleX);
            console.log("viewScaleY: " + viewScaleY);

            const finalScaleX = worldMat.m00 * viewScaleX;
            const finaleScaleY = worldMat.m05 * viewScaleY;
            console.log("worldMat.m00: " + worldMat.m00);
            console.log("worldMat.m05: " + worldMat.m05);
            console.log("finalScaleX: " + finalScaleX);
            console.log("finaleScaleY: " + finaleScaleY);

            const viewportRect = cc.view._viewportRect;
            const offsetX = viewportRect.x / dpr;
                const offsetY = viewportRect.y / dpr;

            console.log("viewportRect.x: " + viewportRect.x);
            console.log("viewportRect.y: " + viewportRect.y);
            console.log("worldMat.m12: " + worldMat.m12);
            console.log("worldMat.m13: " + worldMat.m13);
            console.log("return x: " + worldMat.m12 * viewScaleX + offsetX);
            console.log("return y: " + worldMat.m13 * viewScaleY + offsetY);
            console.log("return width: " + width * finalScaleX);
            console.log("return height: " + height * finaleScaleY);

            return {
                x: worldMat.m12 * viewScaleX + offsetX,
                y: worldMat.m13 * viewScaleY + offsetY,
                width: width * finalScaleX,
                height: height * finaleScaleY,
            };
        }

        _getUVRect() {
            console.log("==========_getUVRect==========");
            const node = this._delegate.node;
            node.getWorldMatrix(worldMat);
            let visibleSize = cc.view.getVisibleSize();
            let uvX = 0;
            let uvY = 0;
            let width = 0;
            let height = 0;
            let uvWidth = 0;
            let uvHeight = 0;
            let originX = worldMat.m12;
            let originY = worldMat.m13;
            
            const transform = node._uiProps.uiTransformComp;
            if (transform) {
                const contentSize = transform.contentSize;
                const anchorPoint = transform.anchorPoint;
                console.log("contentSize: " + contentSize);
                console.log("anchorPoint: " + anchorPoint);

                width = contentSize.width * worldMat.m00;
                height = contentSize.height * worldMat.m05;
                console.log("width: " + width);
                console.log("height: " + height);
                uvWidth = width / visibleSize.width;
                uvHeight = height / visibleSize.height;
                console.log("uvWidth: " + uvWidth);
                console.log("uvHeight: " + uvHeight);

                originX = worldMat.m12 - width * (1 - anchorPoint.x);
                originY = worldMat.m13 - height * (1 - anchorPoint.y);
                console.log("originX: " + originX);
                console.log("originY: " + originY);
            }
            uvX = originX / visibleSize.width;
            uvY = originY / visibleSize.height;
            console.log("uvX: " + uvX);
            console.log("uvY: " + uvY);
            return {
                uvX: uvX,
                uvY: uvY,
                uvWidth: uvWidth,
                uvHeight: uvHeight,
            };
        }
    }
    EditBox._EditBoxImpl = JsbEditBoxImpl;
}());
