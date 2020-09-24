import tensorflow as tf
print(tf.__version__)
print(tf.executing_eagerly())

import cv2
import os
import PIL
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image


model = tf.saved_model.load('saved_model_dir')
concrete_func = model.signatures[tf.saved_model.DEFAULT_SERVING_SIGNATURE_DEF_KEY]
concrete_func.inputs[0].set_shape([1, 256, 256, 3])#

converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

open('whitebox_cartoon_gan_dr.tflite', 'wb').write(tflite_model)


def load_image(path_to_img):
    img = cv2.imread(path_to_img)
    img = img.astype(np.float32) / 127.5 - 1
    img = np.expand_dims(img, 0)
    img = tf.convert_to_tensor(img)
    return img

#Function to pre-process by resizing an central cropping it
# 通过调整中心裁剪的大小来进行预处理的功能
def preprocess_image(image, target_dim=224):
    #Resize the image so that the shorter dimension become 256px
    # 调整图像大小，使较短的尺寸变为256px
    shape = tf.cast(tf.shape(image)[1:-1], tf.float32)
    short_dim = min(shape)
    scale = target_dim / short_dim
    new_shape = tf.cast(shape * scale, tf.int32)
    image = tf.image.resize(image, new_shape)

    #Central crop the image
    # 中央裁剪图像
    image = tf.image.resize_with_crop_or_pad(image, target_dim, target_dim)

    return image

source_image = load_image('./image.jpg')
preprocessed_source_image = preprocess_image(source_image, target_dim=256)
print(preprocessed_source_image.shape)

interpreter = tf.lite.Interpreter(model_path='./whitebox_cartoon_gan_dr.tflite')
input_details = interpreter.get_input_details()
interpreter.allocate_tensors()
interpreter.set_tensor(input_details[0]['index'], preprocessed_source_image)
interpreter.invoke()

raw_prediction = interpreter.tensor(
    interpreter.get_output_details()[0]['index'])()

output = (np.squeeze(raw_prediction) + 1.0)*127.5
output = np.clip(output, 0, 255).astype(np.unit8)
output = cv2.cvtColor(output, cv2.COLOR_BGR2RGB)



hr = Image.fromarray(output)
hr.save('cartoonized_image.jpg')

plt.subplot(2, 1)
plt.imshow(plt.imread('./image.jpg'))
plt.title('Source image')
plt.subplot(1, 2, 2)
plt.imshow(output)
plt.title('Cartoonized image')
plt.show()


