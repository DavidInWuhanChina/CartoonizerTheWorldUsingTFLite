import tensorflow as tf
import cv2
import numpy as np
from PIL import Image
import matplotlib.pyplot as plt
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
output = np.clip(output, 0, 255).astype(np.uint8)
output = cv2.cvtColor(output, cv2.COLOR_BGR2RGB)

hr = Image.fromarray(output)
hr.save('cartoonized_image.jpg')

plt.subplot(1, 2, 1)
plt.imshow(plt.imread('./image.jpg'))
plt.title('Source image')
plt.subplot(1, 2, 2)
plt.imshow(output)
plt.title('Cartoonized image')
plt.show()

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
low_res_image = load_img('./cartoonized_image.jpg', None, None)

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
)

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
