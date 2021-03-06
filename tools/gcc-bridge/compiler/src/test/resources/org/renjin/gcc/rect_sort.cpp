

#include "rect.cpp"

#include <vector>
#include <algorithm>

bool mysort(const Rectangle &x, const Rectangle &y) {
    return x.width < y.width; 
}

extern "C" void test_sort_records() {
    
    std::vector<Rectangle> v;
    v.push_back(Rectangle(9, 0));
    v.push_back(Rectangle(3, 0));    
    v.push_back(Rectangle(10, 0));
    v.push_back(Rectangle(40, 0));
    v.push_back(Rectangle(23, 0));
    v.push_back(Rectangle(99, 0));
    v.push_back(Rectangle(99, 0));
    v.push_back(Rectangle(100, 0));
            
    std::sort(v.begin(), v.end());
    
    ASSERT(v[0].width == 3)
    ASSERT(v[1].width == 9)
    ASSERT(v[2].width == 10)
}

extern "C" void test_mem_copy() {
    
    Rectangle a[3];
    Rectangle b[3];
    
    a[1].width = 10;
    a[1].height = 25;
    
    __builtin_memcpy(b, a, sizeof(Rectangle) * 3);
//    
//    ASSERT(a[1].width == 10)
//    ASSERT(b[1].width == 10)
//
//    // Changes to a should not affect b
//
//    a[1].width = 92;
//    ASSERT(b[1].width == 10)

}