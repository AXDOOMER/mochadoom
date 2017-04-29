package p;

import doom.SourceCode;
import rr.line_t;
import static rr.line_t.ML_TWOSIDED;
import rr.sector_t;
import static utils.C2JUtils.eval;

public interface ActionsPlanes<T, V> extends ActionsSectors<T, V>, ActionsUtility<T, V> {
    /**
     * Move a plane (floor or ceiling) and check for crushing
     *
     * @param sector
     * @param speed fixed
     * @param dest fixed
     * @param crush
     * @param floorOrCeiling
     * @param direction
     */
    default result_e MovePlane(sector_t sector, int speed, int dest, boolean crush, int floorOrCeiling, int direction) {
        final ActionsRegistry<T, V> obs = obs();
        boolean flag;
        @SourceCode.fixed_t int lastpos;

        switch (floorOrCeiling) {
            case 0:
                // FLOOR
                switch (direction) {
                    case -1:
                        // DOWN
                        if (sector.floorheight - speed < dest) {
                            lastpos = sector.floorheight;
                            sector.floorheight = dest;
                            flag = this.ChangeSector(sector, crush);
                            if (flag == true) {
                                sector.floorheight = lastpos;
                                this.ChangeSector(sector, crush);
                                //return crushed;
                            }
                            return result_e.pastdest;
                        } else {
                            lastpos = sector.floorheight;
                            sector.floorheight -= speed;
                            flag = this.ChangeSector(sector, crush);
                            if (flag == true) {
                                sector.floorheight = lastpos;
                                this.ChangeSector(sector, crush);
                                return result_e.crushed;
                            }
                        }
                        break;

                    case 1:
                        // UP
                        if (sector.floorheight + speed > dest) {
                            lastpos = sector.floorheight;
                            sector.floorheight = dest;
                            flag = this.ChangeSector(sector, crush);
                            if (flag == true) {
                                sector.floorheight = lastpos;
                                this.ChangeSector(sector, crush);
                                //return crushed;
                            }
                            return result_e.pastdest;
                        } else {
                            // COULD GET CRUSHED
                            lastpos = sector.floorheight;
                            sector.floorheight += speed;
                            flag = this.ChangeSector(sector, crush);
                            if (flag == true) {
                                if (crush == true) {
                                    return result_e.crushed;
                                }
                                sector.floorheight = lastpos;
                                this.ChangeSector(sector, crush);
                                return result_e.crushed;
                            }
                        }
                        break;
                }
                break;

            case 1:
                // CEILING
                switch (direction) {
                    case -1:
                        // DOWN
                        if (sector.ceilingheight - speed < dest) {
                            lastpos = sector.ceilingheight;
                            sector.ceilingheight = dest;
                            flag = this.ChangeSector(sector, crush);

                            if (flag == true) {
                                sector.ceilingheight = lastpos;
                                this.ChangeSector(sector, crush);
                                //return crushed;
                            }
                            return result_e.pastdest;
                        } else {
                            // COULD GET CRUSHED
                            lastpos = sector.ceilingheight;
                            sector.ceilingheight -= speed;
                            flag = this.ChangeSector(sector, crush);

                            if (flag == true) {
                                if (crush == true) {
                                    return result_e.crushed;
                                }
                                sector.ceilingheight = lastpos;
                                this.ChangeSector(sector, crush);
                                return result_e.crushed;
                            }
                        }
                        break;

                    case 1:
                        // UP
                        if (sector.ceilingheight + speed > dest) {
                            lastpos = sector.ceilingheight;
                            sector.ceilingheight = dest;
                            flag = this.ChangeSector(sector, crush);
                            if (flag == true) {
                                sector.ceilingheight = lastpos;
                                this.ChangeSector(sector, crush);
                                //return crushed;
                            }
                            return result_e.pastdest;
                        } else {
                            lastpos = sector.ceilingheight;
                            sector.ceilingheight += speed;
                            flag = this.ChangeSector(sector, crush);
                            // UNUSED
                            /*
                            if (flag == true)
                            {
                                sector.ceilingheight = lastpos;
                                P_ChangeSector(sector,crush);
                                return crushed;
                            }
                             */
                        }
                        break;
                }
                break;

        }
        return result_e.ok;
    }

    /**
     * Special Stuff that can not be categorized
     *
     * (I'm sure it has something to do with John Romero's obsession with fucking stuff and making them his bitches).
     *
     * @param line
     *
     */
    default boolean DoDonut(line_t line) {
        final ActionsRegistry<T, V> obs = obs();
        sector_t s1;
        sector_t s2;
        sector_t s3;
        int secnum;
        boolean rtn;
        int i;
        floormove_t floor;

        secnum = -1;
        rtn = false;
        while ((secnum = obs.FindSectorFromLineTag(line, secnum)) >= 0) {
            s1 = obs.DOOM.levelLoader.sectors[secnum];

            // ALREADY MOVING?  IF SO, KEEP GOING...
            if (s1.specialdata != null) {
                continue;
            }

            rtn = true;
            s2 = s1.lines[0].getNextSector(s1);
            for (i = 0; i < s2.linecount; i++) {
                if ((!eval(s2.lines[i].flags & ML_TWOSIDED))
                        || (s2.lines[i].backsector == s1)) {
                    continue;
                }
                s3 = s2.lines[i].backsector;

                //  Spawn rising slime
                floor = new floormove_t();
                s2.specialdata = floor;
                floor.thinkerFunction = ActionFunction.T_MoveFloor;
                obs.AddThinker(floor);
                floor.type = floor_e.donutRaise;
                floor.crush = false;
                floor.direction = 1;
                floor.sector = s2;
                floor.speed = ActionsRegistry.FLOORSPEED / 2;
                floor.texture = s3.floorpic;
                floor.newspecial = 0;
                floor.floordestheight = s3.floorheight;

                //  Spawn lowering donut-hole
                floor = new floormove_t();
                s1.specialdata = floor;
                floor.thinkerFunction = ActionFunction.T_MoveFloor;
                obs.AddThinker(floor);
                floor.type = floor_e.lowerFloor;
                floor.crush = false;
                floor.direction = -1;
                floor.sector = s1;
                floor.speed = ActionsRegistry.FLOORSPEED / 2;
                floor.floordestheight = s3.floorheight;
                break;
            }
        }
        return rtn;
    }

}
