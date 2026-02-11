# Immortal Locator (仙定)
A client-side fabric mod which allows you to find stronghold with only one Eye of Ender.

Inspired by [Simple Stronghold Finder](https://modrinth.com/mod/simple-stronghold-finder), fixed some bugs and rewritten in Kotlin.

> The name of this module is inspired by the ancient Chinese legend of a sage pointing the way.  
> 本模组名称灵感来源于中国古代仙人指路传说。

## Features
- Find stronghold with only one Eye of Ender.
- Minecraft 1.21.11 compatible.
- I18n supported.
- A fancy effect when found a stronghold.
- We love immortal locator so much. (仙定仙定我们喜欢你)

## Overview

This tool leverages the behavior of the Eye of Ender in Minecraft Java Edition to locate strongholds efficiently with only one throw of the Eye of Ender. Strongholds in Minecraft generate in rings around the world origin (X=0, Z=0). The locations follow a specific pattern, with a fixed number of strongholds per ring and distributed roughly evenly around the ring.

### Stronghold Generation Rings

![image](https://github.com/user-attachments/assets/01dbf039-3e2d-4ddf-8d5c-4140c4823df8)


There are 8 rings containing a total of 128 strongholds. Below is a breakdown of the stronghold distribution:

| Ring | Number of Strongholds | Distance from Origin (blocks) |
|------|-----------------------|-------------------------------|
| 1    | 3                     | 1,280–2,816                   |
| 2    | 6                     | 4,352–5,888                   |
| 3    | 10                    | 7,424–8,960                   |
| 4    | 15                    | 10,496–12,032                 |
| 5    | 21                    | 13,568–15,104                 |
| 6    | 28                    | 16,640–18,176                 |
| 7    | 36                    | 19,712–21,248                 |
| 8    | 9                     | 22,784–24,320                 |

### Key Stronghold Properties

1. Strongholds are placed at equal angles from the origin within each ring.
2. Strongholds do not generate partially above ground. If any part would extend above sea level, it is replaced with air.
3. Occasionally, strongholds generate in oceans, usually covered by terrain.

## How It Works

This implementation calculates the intersection of the Eye of Ender’s trajectory with the stronghold rings using simple geometric principles. Here is how the process works:

1. **Recording Eye Movement:**

    - The program captures two positions (`pos1` and `pos2`) of the Eye of Ender during its flight. These positions are recorded with a delay to ensure that the trajectory is properly traced.
    - If the Eye travels strictly along one axis (indicating a degenerate case), the calculation is aborted.

2. **Adjusting Coordinates:**

    - To simplify calculations, the coordinate system may be swapped (X becomes Z, and vice versa) depending on which axis the Eye traveled farther along. This ensures the slope of the line remains manageable.

3. **Calculating the Trajectory:**

    - Using the two recorded points, the slope of the trajectory is determined:
    - This slope is used to predict where the trajectory intersects with the Minecraft world grid.

4. **Finding the Ring:**

    - The tool calculates the radial distance of each potential intersection point from the origin

    - It checks whether the distance falls within one of the predefined stronghold rings. The distance ranges for each ring are carefully defined.

5. **Aligning to Minecraft Grid:**

    - Since strongholds are aligned to a 16x16 grid, the calculated coordinates are snapped to the nearest grid point

6. **Evaluating Accuracy:**

    - For each potential stronghold location, an accuracy score is calculated based on how closely the Eye’s trajectory aligns with the grid point. Higher accuracy indicates a higher likelihood of a stronghold being present.

7. **Sorting and Displaying Results:**

    - The program ranks the identified stronghold locations by accuracy. Up to 5 of the best candidates are displayed in the in-game chat, showing:
        - Overworld coordinates (X, Z)
        - Nether coordinates (X/8, Z/8)
        - Accuracy score

## Usage

1. Equip an Eye of Ender.
2. Throw the Eye and wait for the results to appear in the chat.
3. Travel to the provided coordinates to locate the stronghold.
