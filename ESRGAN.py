import tensorflow as tf
import cv2
import numpy as np
from PIL import Image
import matplotlib.pyplot as plt

#Utility for image loading and prepeocessing\
def load_img(path_to_img, scale_factor=4, save_path="downsampled_image.jpg"):
    img = tf.io.read_file(path_to_img)
    img = tf.io.decode_image(img, channels=3)
    img = img.numpy()
    hr = img = Image.fromarray(img)
    if not scale_factor:
        width, height = 512, 512
        scale_factor = 4
    else:
        width, height = img.size
    if save_path:
        lr = img = img.resize(
            (width // scale_factor, height // scale_factor),
            Image.BICUBIC
        )
        # Image.NEAREST ：低质量
        # Image.BILINEAR：双线性
        # Image.BICUBIC ：三次样条插值
        # Image.ANTIALIAS：高质量
        lr.save(save_path)
    img = np.asarray(img)
    img = tf.cast(img, dtype=tf.float32)
    img = img[tf.newaxis, :]
    return img

#Load the image
low_res_image = load_img('./cartoonized_image.jpg', None, './cartoonized_image.jpg')

#Load the model
interpreter = tf.lite.Interpreter(model_path=f'./esrgan_dr.tflite')
interpreter.allocate_tensors()

#Set model Input
input_details = interpreter.get_input_details()
interpreter.allocate_tensors()


#Invoke the interpreter to run inference
interpreter.set_tensor(input_details[0]['index'], low_res_image)
interpreter.invoke()

#Retrieve the enhanced image
# 检索增强的图像
enhanced_img = interpreter.tensor(
    interpreter.get_output_details()[0]['index']
)()

def get_concat_h(im1, im2):
    dst = Image.new('RGB', (im1.width + im2.width, im1.height))
    dst.paste(im1, (0, 0))
    dst.paste(im2, (im1.width, 0))
    return dst

a = tf.cast(tf.clip_by_value(enhanced_img[0], 0, 255), tf.uint8)
super_resolution_img = Image.fromarray(a.numpy(), 'RGB')
super_resolution_img = super_resolution_img.resize((512, 512))

down_sampled_image = Image.open('./cartoonized_image.jpg').resize((512, 512))

get_concat_h(down_sampled_image, super_resolution_img)


